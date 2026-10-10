package io.ddd4j.javalin.mq.redisstream;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.redisstream.RedisStreamMQClient;
import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq RedisStreamMQClient (RedisStreamMQProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jRedisStreamMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final RedisStreamMQClient client;

    /**
     * 构造 Ddd4jRedisStreamMqGuiceModule Guice 模块实例。
     * @param client MQ 客户端实例。
     */
    public Ddd4jRedisStreamMqGuiceModule(RedisStreamMQClient client) {
        this(client, new RedisStreamMQProperties());
    }

    /**
     * 构造 Ddd4jRedisStreamMqGuiceModule Guice 模块实例。
     * @param client MQ 客户端实例。
     * @param properties 配置属性。
     */
    public Ddd4jRedisStreamMqGuiceModule(RedisStreamMQClient client, RedisStreamMQProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    /**
     * 装配 Guice 绑定。
     */
    @Override
    protected void configure() {
        super.configure();
        bind(RedisStreamMQProperties.class).toInstance((RedisStreamMQProperties) mqProperties());
        bind(RedisStreamMQClient.class).toInstance(client);
    }

    /**
     * 提供 MQ 客户端。
     * @return MQ 客户端实例。
     */
    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
