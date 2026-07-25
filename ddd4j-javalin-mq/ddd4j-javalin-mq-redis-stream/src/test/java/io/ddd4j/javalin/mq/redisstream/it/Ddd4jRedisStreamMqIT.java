package io.ddd4j.javalin.mq.redisstream.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.redisstream.Ddd4jRedisStreamMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.database.RedisTestContainerFixture;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.redisstream.RedisStreamMQBrokerAdapter;
import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link Ddd4jRedisStreamMqGuiceModule} against a real Redis instance
 * brought up by Testcontainers.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRedisStreamMqIT {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new RedisTestContainerFixture().newContainer();

    @Test
    void shouldResolveCoreContractsFromGuice() {
        REDIS.start();
        try {
            RedisStreamMQProperties redisProps = new RedisStreamMQProperties();
            redisProps.setHost(REDIS.getHost());
            redisProps.setPort(REDIS.getMappedPort(RedisTestContainerFixture.DEFAULT_PORT));
            Ddd4jMQProperties mqProps = new Ddd4jMQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("redisStream");

            Injector injector = Guice.createInjector(
                    new Ddd4jRedisStreamMqGuiceModule(redisProps, mqProps));

            assertThat(injector.getInstance(MQEventPublisher.class)).isNotNull();
            assertThat(injector.getInstance(MQBrokerAdapter.class)).isNotNull();
            assertThat(injector.getInstance(RedisStreamMQBrokerAdapter.class)).isNotNull();
        } finally {
            REDIS.stop();
        }
    }
}