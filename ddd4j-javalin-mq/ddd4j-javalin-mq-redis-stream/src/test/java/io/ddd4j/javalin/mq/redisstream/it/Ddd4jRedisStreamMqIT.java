package io.ddd4j.javalin.mq.redisstream.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.redisstream.Ddd4jRedisStreamMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.database.RedisTestContainerFixture;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.mq.redisstream.RedisStreamMQClient;
import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

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
}
