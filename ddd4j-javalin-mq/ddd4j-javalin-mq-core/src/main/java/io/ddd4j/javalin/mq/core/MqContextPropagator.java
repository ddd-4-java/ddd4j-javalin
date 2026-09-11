package io.ddd4j.javalin.mq.core;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 跨 MQ 发布/消费线程的 {@link ThreadContext} 透传器（基于进程级 msgId 快照表）。
 *
 * <p><b>解决痛点</b>：{@link ThreadContext} 已基于
 * {@code com.alibaba.ttl.TransmittableThreadLocal}，但只能透传到由
 * {@code TtlRunnable} / {@code TtlCallable} 包装的线程池任务。<b>MQ broker
 * （Rabbit / Kafka / RocketMQ / Redis Stream ...）的消费线程不由 TTL 接管</b>，
 * 导致 {@code ThreadContext.getSubject()} 等上下文在 broker worker 线程
 * 始终为 {@code null}。</p>
 *
 * <p><b>本类策略</b>：
 * <ol>
 *   <li>Publisher 端：每次 {@code producer.accept(event)} 之前 capture
 *       {@code ThreadContext.getResources()}，存入进程级
 *       {@link BaseContext}{@code Map<msgId, TimedSnapshot>}（O(1) 写入）。</li>
 *   <li>Consumer 端：用 {@link Proxy} 包装 {@link MQListener#getBean()}，
 *       invoke 前按 {@link MQEvent#getMsgId()} 取 snapshot 并装入
 *       {@link ThreadContext}，invoke 后清空。</li>
 * </ol>
 *
 * <p><b>降级策略</b>：若 {@code msgId} 为空、snapshot 已被清理或已过期、
 * registry 容量达上限，{@link #restore} 返回 {@code null}，由调用方
 * 决定是否走"无上下文"路径（保留原始行为，保证向下兼容）。</p>
 *
 * <p><b>内存安全（v6.7.x+）</b>：registry 引入两层保护：
 * <ul>
 *   <li><b>条目级 TTL</b>：snapshot 写入时记时间戳；consumer 读时若
 *       {@code now - captureTimestamp > snapshotTtlMillis} 则丢弃并 remove，
 *       避免 broker 永久不消费导致累积。</li>
 *   <li><b>容量上限</b>：registry 容量达到 {@code maxSnapshots} 时拒绝
 *       capture（log warn，不抛异常，保证业务连续性）。</li>
 * </ul>
 *
 * <p>TTL 与上限默认值分别为 5 分钟 / 10000 条；可通过 {@link BaseContext} 中
 * {@link #SNAPSHOT_TTL_MILLIS_KEY} / {@link #MAX_SNAPSHOTS_KEY} 覆盖。
 * 优先级：BaseContext KV &gt; 系统默认值。</p>
 *
 * <p><b>线程安全</b>：registry 使用 {@link ConcurrentHashMap}；
 * producer 端 capture 与 consumer 端 remove 通过 {@link MQEvent#getMsgId()}
 * 串接；当前实现采用"消费即移除"语义，依靠 broker 至少一次投递保证；
 * 若业务需要 at-least-once 严格保留，建议在自定义消费装饰器中显式 ack
 * 后调 {@link #discard(String)}。</p>
 *
 * @author ddd4j-javalin
 * @since 6.7.x
 */
@Slf4j
final class MqContextPropagator {

    /**
     * {@link BaseContext} 中 snapshot 表的 key。
     */
    static final String SNAPSHOT_REGISTRY_KEY = MqContextPropagator.class.getName() + ".snapshots";

    /**
     * {@link BaseContext} 中 TTL 覆盖 key（{@link Long}，毫秒）。默认 5 分钟。
     */
    static final String SNAPSHOT_TTL_MILLIS_KEY = MqContextPropagator.class.getName() + ".ttlMillis";

    /**
     * {@link BaseContext} 中容量上限覆盖 key（ {@link Integer}）。默认 10000。
     */
    static final String MAX_SNAPSHOTS_KEY = MqContextPropagator.class.getName() + ".maxSnapshots";

    /**
     * 默认 snapshot TTL：5 分钟。
     */
    static final long DEFAULT_SNAPSHOT_TTL_MILLIS = 5 * 60 * 1000L;

    /**
     * 默认 registry 容量上限：10000 条。
     */
    static final int DEFAULT_MAX_SNAPSHOTS = 10000;

    /**
     * 包装 snapshot + 写入时间戳，使 consumer 端可以做 TTL 判断。
     */
    private static final class TimedSnapshot {

        final long captureMillis;
        final Map<Object, Object> resources;

        TimedSnapshot(long captureMillis, Map<Object, Object> resources) {
            this.captureMillis = captureMillis;
            this.resources = resources;
        }

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - captureMillis > ttlMillis;
        }
    }

    /**
     * 不可快照作用域：进入时直接 {@code ThreadContext.setResources(snapshot)}
     * （与 {@link ThreadContext#open(Map)} 不同——后者在 close 时恢复原值，
     * 我们的语义是"消费完彻底清理，避免 broker 线程复用串扰"）。
     */
    private static final class SnapshotScope implements AutoCloseable {

        private final Map<Object, Object> snapshot;
        private boolean closed;

        private SnapshotScope(Map<Object, Object> snapshot) {
            this.snapshot = Objects.requireNonNull(snapshot, "snapshot must not be null");
            ThreadContext.setResources(this.snapshot);
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            ThreadContext.clear();
            closed = true;
        }
    }

    private MqContextPropagator() {
    }

    /**
     * 包内访问点：发布前 capture 当前线程上下文快照，按 msgId 索引。
     *
     * <p>保护策略：
     * <ul>
     *   <li>msgId 为空 / snapshot 为空 Map → 静默跳过（降级）</li>
     *   <li>registry 容量达到 {@code maxSnapshots} → 拒绝 capture 并
     *       log warn（不抛异常，避免业务中断）</li>
     * </ul>
     */
    static void capture(MQEvent event) {
        if (Objects.isNull(event) || Objects.isNull(event.getMsgId()) || event.getMsgId().isEmpty()) {
            return;
        }
        Map<Object, Object> snapshot = ThreadContext.getResources();
        if (snapshot.isEmpty()) {
            return;
        }
        Map<String, TimedSnapshot> registry = registry();
        int maxSnapshots = maxSnapshots();
        if (maxSnapshots > 0 && registry.size() >= maxSnapshots) {
            log.warn("MqContextPropagator: snapshot registry reached capacity ({}), dropping capture for msgId [{}] " +
                            "to prevent unbounded memory growth. Consider raising maxSnapshots or check broker consumer.",
                    maxSnapshots, event.getMsgId());
            return;
        }
        registry.put(event.getMsgId(), new TimedSnapshot(System.currentTimeMillis(), snapshot));
        if (log.isDebugEnabled()) {
            log.debug("Captured ThreadContext snapshot for MQ msgId [{}], keys={}",
                    event.getMsgId(), snapshot.keySet());
        }
    }

    /**
     * 包内访问点：消费时按 msgId 取快照并装入当前线程 {@link ThreadContext}。
     *
     * <p>保护策略：snapshot 写入时间超过 TTL 时拒绝使用并 remove（避免
     * broker 永久不消费导致的累积）。</p>
     */
    static AutoCloseable restore(MQEvent event) {
        if (Objects.isNull(event) || Objects.isNull(event.getMsgId()) || event.getMsgId().isEmpty()) {
            return null;
        }
        Map<String, TimedSnapshot> registry = registry();
        TimedSnapshot timed = registry.get(event.getMsgId());
        if (Objects.isNull(timed)) {
            return null;
        }
        long ttl = snapshotTtlMillis();
        if (timed.isExpired(ttl)) {
            // 过期：拒绝使用并清理，避免 broker 永久不消费时累积
            registry.remove(event.getMsgId());
            if (log.isDebugEnabled()) {
                log.debug("Discarded expired snapshot for MQ msgId [{}] (age > {} ms)", event.getMsgId(), ttl);
            }
            return null;
        }
        // 真正消费成功后再 remove（防止 requeue 重投时丢失）
        registry.remove(event.getMsgId());
        if (log.isDebugEnabled()) {
            log.debug("Restored ThreadContext snapshot for MQ msgId [{}], keys={}",
                    event.getMsgId(), timed.resources.keySet());
        }
        return new SnapshotScope(timed.resources);
    }

    /**
     * 包内访问点：消息重投时（如 RabbitMQ requeue）回填 snapshot。
     * 一般不需要手动调用，默认 {@link #restore(MQEvent)} 已
     * 同时 remove；此处提供手动兜底用于自定义 ack 装饰器。
     */
    @SuppressWarnings("unused")
    static void discard(String msgId) {
        if (Objects.nonNull(msgId) && !msgId.isEmpty()) {
            registry().remove(msgId);
        }
    }

    /**
     * 包内访问点：批量清理（运维/调试用），删除所有过期 snapshot。
     * 可周期性调用以回收内存。
     */
    static int purgeExpired() {
        Map<String, TimedSnapshot> registry = registry();
        long ttl = snapshotTtlMillis();
        int removed = 0;
        for (Map.Entry<String, TimedSnapshot> entry : registry.entrySet()) {
            if (entry.getValue().isExpired(ttl)) {
                registry.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0 && log.isDebugEnabled()) {
            log.debug("MqContextPropagator: purged {} expired snapshots", removed);
        }
        return removed;
    }

    /**
     * 包内访问点：包装 {@link Consumer}{@code <MQEvent>} producer，
     * 在 accept 前 capture 当前线程上下文。
     */
    static Consumer<MQEvent> wrap(Consumer<MQEvent> delegate) {
        if (Objects.isNull(delegate)) {
            return null;
        }
        return event -> {
            capture(event);
            delegate.accept(event);
        };
    }

    /**
     * 包内访问点：用 JDK {@link Proxy} 包装 listener bean，
     * invoke 前按 msgId 重建 ThreadContext，invoke 后自动恢复。
     * 任何失败（snapshot 缺失 / 过期）都降级为"无上下文"调用。
     */
    static Object wrapListenerBean(Object originalBean, MQListener listener) {
        if (Objects.isNull(originalBean)) {
            return null;
        }
        // listener 方法签名必为 (MQEvent 或子类)，方便反查 msgId
        Class<?>[] interfaces = originalBean.getClass().getInterfaces();
        if (interfaces.length == 0) {
            // 非接口 bean：保持原状（Guice 代理对象通常是接口代理）
            return originalBean;
        }
        return Proxy.newProxyInstance(
                originalBean.getClass().getClassLoader(),
                interfaces,
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (args != null && args.length == 1 && args[0] instanceof MQEvent) {
                            MQEvent event = (MQEvent) args[0];
                            AutoCloseable scope = restore(event);
                            try {
                                if (Objects.nonNull(scope)) {
                                    try {
                                        return method.invoke(originalBean, args);
                                    } catch (java.lang.reflect.InvocationTargetException ite) {
                                        throw ite.getCause();
                                    }
                                }
                                // 无快照降级
                                try {
                                    return method.invoke(originalBean, args);
                                } catch (java.lang.reflect.InvocationTargetException ite) {
                                    throw ite.getCause();
                                }
                            } finally {
                                if (Objects.nonNull(scope)) {
                                    scope.close();
                                }
                            }
                        }
                        // 非 MQEvent 参数方法（如 hashCode/equals/toString）：透传
                        try {
                            return method.invoke(originalBean, args);
                        } catch (java.lang.reflect.InvocationTargetException ite) {
                            throw ite.getCause();
                        }
                    }
                });
    }

    private static Map<String, TimedSnapshot> registry() {
        Map<String, TimedSnapshot> registry = BaseContext.get(SNAPSHOT_REGISTRY_KEY);
        if (Objects.isNull(registry)) {
            registry = new ConcurrentHashMap<>();
            BaseContext.inject(SNAPSHOT_REGISTRY_KEY, registry);
        }
        return registry;
    }

    private static long snapshotTtlMillis() {
        Long override = BaseContext.get(SNAPSHOT_TTL_MILLIS_KEY);
        return Objects.nonNull(override) && override > 0 ? override : DEFAULT_SNAPSHOT_TTL_MILLIS;
    }

    private static int maxSnapshots() {
        Integer override = BaseContext.get(MAX_SNAPSHOTS_KEY);
        return Objects.nonNull(override) && override > 0 ? override : DEFAULT_MAX_SNAPSHOTS;
    }
}