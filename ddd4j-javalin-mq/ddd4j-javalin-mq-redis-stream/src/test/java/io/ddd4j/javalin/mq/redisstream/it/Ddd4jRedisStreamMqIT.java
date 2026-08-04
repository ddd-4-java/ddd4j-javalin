package io.ddd4j.javalin.mq.redisstream.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.redisstream.Ddd4jRedisStreamMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.database.RedisTestContainerFixture;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jRedisStreamMqGuiceModule} against a real Redis instance
 * brought up by Testcontainers.
 *
 * <p>Verifies a real publish → Redis Stream → consume round trip through
 * {@link RedisStreamMQClient}: the event is XADDed to the {@code topic:tag} stream key,
 * picked up by the consumer group and delivered to the {@link MQEventListener} bean.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRedisStreamMqIT {

    private static final String TOPIC = "ddd4j.it.redis";
    private static final String TAG = "smoke";

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new RedisTestContainerFixture().newContainer();

    @Test
    void shouldResolveCoreContractsFromGuice() {
        REDIS.start();
        try {
            RedisStreamMQProperties redisProps = new RedisStreamMQProperties();
            redisProps.setUrl("redis://" + REDIS.getHost() + ":"
                    + REDIS.getMappedPort(RedisTestContainerFixture.DEFAULT_PORT));
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("redisStream");

            Injector injector = Guice.createInjector(
                    new Ddd4jRedisStreamMqGuiceModule(new RedisStreamMQClient(redisProps), redisProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(RedisStreamMQClient.class)).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(redisProps);
        } finally {
            REDIS.stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        REDIS.start();
        try {
            RedisStreamMQProperties redisProps = new RedisStreamMQProperties();
            redisProps.setUrl("redis://" + REDIS.getHost() + ":"
                    + REDIS.getMappedPort(RedisTestContainerFixture.DEFAULT_PORT));
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("redisStream");
            mqProps.setPersist(false);

            RedisStreamMQClient client = new RedisStreamMQClient(redisProps);
            Injector injector = Guice.createInjector(
                    new Ddd4jRedisStreamMqGuiceModule(client, redisProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            mqClient.init(List.of(listener), mqProps, new JsonMQEventSerialization(), null);

            MQEvent event = new MQEvent();
            event.setMsgId("redis-it-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(10)).until(() -> bean.received.get() != null);
            MQEvent received = bean.received.get();
            assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            REDIS.stop();
        }
    }

    /**
     * Listener bean invoked by the ddd4j consume pipeline; records the delivered event.
     */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "it-redis-consumer")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}
