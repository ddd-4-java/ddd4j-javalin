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

    public Ddd4jRedisStreamMqGuiceModule(RedisStreamMQClient client) {
        this(client, new RedisStreamMQProperties());
    }

    public Ddd4jRedisStreamMqGuiceModule(RedisStreamMQClient client, RedisStreamMQProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(RedisStreamMQProperties.class).toInstance((RedisStreamMQProperties) mqProperties());
        bind(RedisStreamMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
