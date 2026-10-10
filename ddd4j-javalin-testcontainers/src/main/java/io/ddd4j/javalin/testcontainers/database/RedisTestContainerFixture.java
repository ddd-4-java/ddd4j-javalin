package io.ddd4j.javalin.testcontainers.database;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Shared Redis container fixture.
 *
 * <p>Uses {@code redis:7.4.11-alpine} exposing the default 6379 port with
 * {@code redis-cli ping} waiting strategy (PONG response). Suitable for both
 * {@code ddd4j-cache} (Redis-backed {@code CacheKit}) and {@code ddd4j-mq-redis-stream}
 * (Redis Streams) integration tests.
 */
public class RedisTestContainerFixture extends AbstractTestContainerFixture<GenericContainer<?>> {

    /** 默认容器镜像（钉版；原浮动别名 7-alpine 与该补丁标签 digest 一致）。 */
    // 测试镜像来源: https://testcontainers.com/modules/redis/
    // 说明: redis 模块页列出了 Java 支持，但 org.testcontainers 无第一方 redis 工件（社区实现在 com.redis），故保留 GenericContainer。
    public static final String DEFAULT_IMAGE = "redis:7.4.11-alpine";
    /** 默认映射端口。 */
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