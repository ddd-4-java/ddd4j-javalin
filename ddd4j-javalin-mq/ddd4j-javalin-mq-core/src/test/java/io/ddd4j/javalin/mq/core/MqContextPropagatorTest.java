package io.ddd4j.javalin.mq.core;

import io.ddd4j.core.constant.ContextConstants;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.mq.event.MQEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates ThreadContext propagation across MQ publish/ consume threads.
 *
 * <p>Simulates the broker worker thread (different {@code Thread} instance) by
 * invoking the consumer-side wrapper from a separate executor and asserting
 * the captured {@link ThreadContext} snapshot reaches the listener bean.</p>
 */
class MqContextPropagatorTest {

    /** Test-only sentinel used to simulate a {@code Subject} payload in {@link ThreadContext}. */
    static final String CAPTURABLE_KEY = MqContextPropagatorTest.class.getName() + ".capturable";

    @AfterEach
    void cleanup() {
        BaseContext.remove(MqContextPropagator.SNAPSHOT_REGISTRY_KEY);
        ThreadContext.clear();
    }

    @Test
    void shouldRestoreThreadContextSnapshotAcrossPublishAndConsume() {
        String subject = "subject-" + UUID.randomUUID();
        String tenant = "tenant-" + UUID.randomUUID();
        ThreadContext.put(CAPTURABLE_KEY, subject);
        ThreadContext.set(ContextConstants.TENANT_ID, tenant);

        MQEvent event = new MQEvent();
        event.setTopic("orders.created");
        String msgId = "msg-" + UUID.randomUUID();
        event.setMsgId(msgId);

        AtomicReference<String> consumedCapturable = new AtomicReference<>();
        AtomicReference<String> consumedTenant = new AtomicReference<>();
        // 模拟真实 broker worker 线程消费：调 restore 拿 scope，
        // 在 scope 内读 ThreadContext（Subject + tenant 应可见），
        // 关闭 scope 后再次断言：registry 中 msgId 已清除（restore 内部已 remove）。
        Consumer<MQEvent> wrappedProducer = MqContextPropagator.wrap(event1 -> {
            MqContextPropagator.capture(event1);
            try (AutoCloseable ignored = MqContextPropagator.restore(event1)) {
                consumedCapturable.set(ThreadContext.get(CAPTURABLE_KEY));
                consumedTenant.set(ThreadContext.get(ContextConstants.TENANT_ID));
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        wrappedProducer.accept(event);

        assertThat(consumedCapturable.get()).isEqualTo(subject);
        assertThat(consumedTenant.get()).isEqualTo(tenant);
        // restore() 内部已 registry.remove(msgId)
        Map<String, Map<Object, Object>> registry = BaseContext.get(MqContextPropagator.SNAPSHOT_REGISTRY_KEY);
        assertThat(registry).doesNotContainKey(msgId);
    }

    @Test
    void shouldDegradeGracefullyWhenMsgIdMissing() {
        ThreadContext.put(CAPTURABLE_KEY, "subject-A");
        MQEvent event = new MQEvent();
        event.setTopic("orders.test");
        MqContextPropagator.capture(event);
        assertThat(BaseContext.<String, Map<Object, Object>>get(MqContextPropagator.SNAPSHOT_REGISTRY_KEY))
                .isNullOrEmpty();
        assertThat(MqContextPropagator.restore(event)).isNull();
    }

    @Test
    void shouldDegradeGracefullyWhenSnapshotMissing() {
        MQEvent event = new MQEvent();
        event.setTopic("orders.test");
        event.setMsgId("non-existent");
        assertThat(MqContextPropagator.restore(event)).isNull();
    }

    @Test
    void shouldWrapListenerBeanAndRestoreSnapshotBeforeInvoke() throws Exception {
        ThreadContext.put(CAPTURABLE_KEY, "subject-B");
        MQEvent event = new MQEvent();
        event.setTopic("orders.created");
        event.setMsgId("msg-wrap");
        MqContextPropagator.capture(event);

        CapturableBean original = new CapturableBean();
        Object wrapped = MqContextPropagator.wrapListenerBean(original, null);
        assertThat(wrapped).isNotSameAs(original);

        AtomicReference<String> seen = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                wrapped.getClass().getMethod("handle", MQEvent.class).invoke(wrapped, event);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            seen.set(original.lastSeenCapturable);
        });
        worker.start();
        worker.join();

        assertThat(seen.get()).isEqualTo("subject-B");
    }

    @Test
    void shouldWrapListenerBeanFallbackWhenNoSnapshot() throws Exception {
        MQEvent event = new MQEvent();
        event.setTopic("orders.created");
        event.setMsgId("no-snapshot-msg");

        CapturableBean original = new CapturableBean();
        Object wrapped = MqContextPropagator.wrapListenerBean(original, null);

        Thread worker = new Thread(() -> {
            try {
                wrapped.getClass().getMethod("handle", MQEvent.class).invoke(wrapped, event);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        worker.start();
        worker.join();

        assertThat(original.invoked).isTrue();
        assertThat(original.lastSeenCapturable).isNull();
    }

    /**
     * Fix B：超过 maxSnapshots 上限时拒绝 capture（log warn，不抛异常）。
     */
    @Test
    void shouldRejectCaptureWhenRegistryAtCapacity() {
        BaseContext.inject(MqContextPropagator.MAX_SNAPSHOTS_KEY, 2);
        try {
            MQEvent event1 = newEvent("msg-1");
            MQEvent event2 = newEvent("msg-2");
            MQEvent event3 = newEvent("msg-3");

            ThreadContext.put(CAPTURABLE_KEY, "subject");
            MqContextPropagator.capture(event1);
            MqContextPropagator.capture(event2);
            // 第三次应被拒绝
            MqContextPropagator.capture(event3);

            Map<String, ?> registry = BaseContext.get(MqContextPropagator.SNAPSHOT_REGISTRY_KEY);
            assertThat(registry).hasSize(2);
            assertThat(registry).containsOnlyKeys("msg-1", "msg-2");
        } finally {
            BaseContext.remove(MqContextPropagator.MAX_SNAPSHOTS_KEY);
            ThreadContext.clear();
        }
    }

    /**
     * Fix B：超过 TTL 的 snapshot 在 restore 阶段被丢弃并 remove。
     */
    @Test
    void shouldDiscardExpiredSnapshotOnRestore() {
        BaseContext.inject(MqContextPropagator.SNAPSHOT_TTL_MILLIS_KEY, 50L);
        try {
            ThreadContext.put(CAPTURABLE_KEY, "subject");
            MQEvent event = newEvent("msg-ttl");
            MqContextPropagator.capture(event);

            // 等待 TTL 过期
            try {
                Thread.sleep(120L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertThat(MqContextPropagator.restore(event)).isNull();
            Map<String, ?> registry = BaseContext.get(MqContextPropagator.SNAPSHOT_REGISTRY_KEY);
            // expired snapshot 已被 restore 阶段清理
            assertThat(registry).doesNotContainKey("msg-ttl");
        } finally {
            BaseContext.remove(MqContextPropagator.SNAPSHOT_TTL_MILLIS_KEY);
            ThreadContext.clear();
        }
    }

    /**
     * Fix B：BaseContext 覆盖 TTL / maxSnapshots 后行为正确（默认值 vs 覆盖值）。
     */
    @Test
    void shouldHonorBaseContextOverridesForTtlAndCapacity() {
        BaseContext.inject(MqContextPropagator.MAX_SNAPSHOTS_KEY, 0);
        try {
            // override 0 → 视为无效，回落到默认 10000
            ThreadContext.put(CAPTURABLE_KEY, "subject");
            for (int i = 0; i < 11; i++) {
                MQEvent e = newEvent("msg-" + i);
                MqContextPropagator.capture(e);
            }
            Map<String, ?> registry = BaseContext.get(MqContextPropagator.SNAPSHOT_REGISTRY_KEY);
            assertThat(registry).hasSize(11);
        } finally {
            BaseContext.remove(MqContextPropagator.MAX_SNAPSHOTS_KEY);
            ThreadContext.clear();
        }
    }

/**
     * Fix B：purgeExpired 清理所有过期 snapshot（运维手动调用）。
     *
     * <p>本测试只验证"以当前 TTL 配置判定哪些快照已过期并 remove"——
     * 通过先 capture 一批小 TTL 快照 → sleep 超时 → 调 purgeExpired，
     * 预期全部清掉。"新鲜快照不被误清"语义已由
     * {@link #shouldDiscardExpiredSnapshotOnRestore} 覆盖。</p>
     */
    @Test
    void shouldPurgeExpiredSnapshots() {
        BaseContext.inject(MqContextPropagator.SNAPSHOT_TTL_MILLIS_KEY, 50L);
        try {
            ThreadContext.put(CAPTURABLE_KEY, "subject");
            MqContextPropagator.capture(newEvent("msg-a"));
            MqContextPropagator.capture(newEvent("msg-b"));

            try {
                Thread.sleep(120L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int purged = MqContextPropagator.purgeExpired();
            assertThat(purged).isEqualTo(2);

            Map<String, ?> registry = BaseContext.get(MqContextPropagator.SNAPSHOT_REGISTRY_KEY);
            assertThat(registry).doesNotContainKeys("msg-a", "msg-b");
        } finally {
            BaseContext.remove(MqContextPropagator.SNAPSHOT_TTL_MILLIS_KEY);
            ThreadContext.clear();
        }
    }

    private static MQEvent newEvent(String msgId) {
        MQEvent event = new MQEvent();
        event.setTopic("orders.test");
        event.setMsgId(msgId);
        return event;
    }

    interface CapturableBeanInterface {
        void handle(MQEvent event);
    }

    static final class CapturableBean implements CapturableBeanInterface {
        volatile boolean invoked;
        volatile String lastSeenCapturable;

        @Override
        public void handle(MQEvent event) {
            this.invoked = true;
            this.lastSeenCapturable = ThreadContext.get(CAPTURABLE_KEY);
        }
    }
}
