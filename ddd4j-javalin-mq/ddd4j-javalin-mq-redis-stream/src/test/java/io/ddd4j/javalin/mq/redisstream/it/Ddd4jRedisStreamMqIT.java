package io.ddd4j.javalin.mq.redisstream.it;

import com.google.inject.Guice;
import com.google.inject.Module;
import io.ddd4j.javalin.mq.redisstream.Ddd4jRedisStreamMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.database.RedisTestContainerFixture;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.redisstream.RedisStreamMQClient;
import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jRedisStreamMqGuiceModule} against a real Redis instance
 * brought up by Testcontainers. 公共骨架继承自 {@link AbstractMqIntegrationTest}：
 * 事件被 XADD 到 {@code topic:tag} stream key，由 consumer group 拾取并投递到
 * {@link io.ddd4j.mq.annotation.MQEventListener} bean。
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRedisStreamMqIT extends AbstractMqIntegrationTest<RedisStreamMQProperties, RedisStreamMQClient> {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new RedisTestContainerFixture().newContainer();

    @Override
    protected String brokerName() {
        return "redisStream";
    }

    @Override
    protected String topicName() {
        return "ddd4j.it.redis";
    }

    @Override
    protected GenericContainer<?> container() {
        return REDIS;
    }

    @Override
    protected RedisStreamMQProperties newProperties() {
        RedisStreamMQProperties props = new RedisStreamMQProperties();
        props.setUrl("redis://" + REDIS.getHost() + ":"
                + REDIS.getMappedPort(RedisTestContainerFixture.DEFAULT_PORT));
        return props;
    }

    @Override
    protected RedisStreamMQClient newClient(RedisStreamMQProperties props) {
        return new RedisStreamMQClient(props);
    }

    @Override
    protected Module guiceModule(RedisStreamMQClient client, RedisStreamMQProperties props) {
        return new Ddd4jRedisStreamMqGuiceModule(client, props);
    }

    @Override
    protected Class<RedisStreamMQClient> clientClass() {
        return RedisStreamMQClient.class;
    }

    @Override
    protected Duration consumerSettleDelay() {
        // consumer group 在 init 内同步创建（XGROUP CREATE），无需额外等待
        return Duration.ZERO;
    }

    @Override
    protected Duration awaitTimeout() {
        return Duration.ofSeconds(10);
    }

    @Test
    void shouldRecoverPendingMessageAfterPersistenceFailure() throws Exception {
        REDIS.start();
        RedisStreamMQClient client = null;
        try {
            RedisStreamMQProperties redisProperties = newProperties();
            MQProperties properties = new MQProperties();
            properties.setEnabled(true);
            properties.setBroker(brokerName());
            properties.setPersist(true);
            client = new RedisStreamMQClient(redisProperties);
            MQClient mqClient = Guice.createInjector(guiceModule(client, redisProperties)).getInstance(MQClient.class);
            RecoveryListener handler = new RecoveryListener();
            Method method = RecoveryListener.class.getMethod("onMessage", MQEvent.class);
            MQListener listener = MQListener.of(handler, method, method.getAnnotation(MQEventListener.class));
            listener.setTopic(topicName());
            listener.setTags(TAG);
            listener.setGroup("it-redis-recovery-consumer");
            AtomicInteger storeAttempts = new AtomicInteger();
            mqClient.init(Collections.singletonList(listener), properties, new JsonMQEventSerialization(), event -> {
                if (storeAttempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("simulated persistence outage");
                }
            });
            MQEvent event = new MQEvent();
            event.setMsgId("redis-recovery-it-" + System.nanoTime());
            event.setTopic(topicName());
            event.setTag(TAG);

            event.publish();

            await().atMost(Duration.ofSeconds(15)).until(() -> Objects.nonNull(handler.received.get()));
            assertThat(storeAttempts.get()).isGreaterThanOrEqualTo(2);
            assertThat(handler.invocations.get()).isEqualTo(1);
            assertThat(handler.received.get().getMsgId()).isEqualTo(event.getMsgId());
        } finally {
            if (Objects.nonNull(client)) {
                client.close();
            }
            REDIS.stop();
        }
    }

    public static class RecoveryListener {
        final AtomicReference<MQEvent> received = new AtomicReference<>();
        final AtomicInteger invocations = new AtomicInteger();

        @MQEventListener(topic = "", tags = TAG, group = "")
        public void onMessage(MQEvent event) {
            invocations.incrementAndGet();
            received.set(event);
        }
    }
}
