package io.ddd4j.javalin.testcontainers.database;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Shared Redis container fixture.
 *
 * <p>Uses {@code redis:7-alpine} exposing the default 6379 port with
 * {@code redis-cli ping} waiting strategy (PONG response). Suitable for both
 * {@code ddd4j-cache} (Redis-backed {@code CacheKit}) and {@code ddd4j-mq-redis-stream}
 * (Redis Streams) integration tests.
 */
public class RedisTestContainerFixture extends AbstractTestContainerFixture<GenericContainer<?>> {
    /**
     * 默认构造方法：Fixture 仅持有镜像与端口常量，容器由 {@link #newContainer()} 构建。
     */
    public RedisTestContainerFixture() {
    }

    /** 默认容器镜像。 */
    public static final String DEFAULT_IMAGE = "redis:7-alpine";
    /** 容器暴露的端口。 */
    public static final int DEFAULT_PORT = 6379;

    @Override
    public GenericContainer<?> newContainer() {
        return new GenericContainer<>(DEFAULT_IMAGE)
                .withExposedPorts(DEFAULT_PORT)
                .waitingFor(Wait.forListeningPort())
                .withReuse(true);
    }

    @Override
    protected String resolveConnectionString(GenericContainer<?> container) {
        return String.format("redis://%s:%d",
                container.getHost(), container.getMappedPort(DEFAULT_PORT));
    }
}