package io.ddd4j.javalin.mq.redisstream;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.redisstream.RedisStreamMQBrokerAdapter;
import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
import io.ddd4j.mq.redisstream.RedisStreamOperations;
import io.ddd4j.mq.spi.MQBrokerAdapter;

import java.util.Objects;

/**
 * Javalin Redis Stream Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jRedisStreamMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final RedisStreamMQProperties redisProperties;
    private final RedisStreamOperations operations;
    private final RedisStreamMQBrokerAdapter brokerAdapter;

    public Ddd4jRedisStreamMqGuiceModule() {
        this(new RedisStreamMQProperties());
    }

    public Ddd4jRedisStreamMqGuiceModule(RedisStreamMQProperties redisProperties) {
        this(redisProperties, new Ddd4jMQProperties(), null);
    }

    public Ddd4jRedisStreamMqGuiceModule(RedisStreamMQProperties redisProperties, Ddd4jMQProperties mqProperties) {
        this(redisProperties, mqProperties, null);
    }

    public Ddd4jRedisStreamMqGuiceModule(RedisStreamMQProperties redisProperties,
                                         Ddd4jMQProperties mqProperties,
                                         RedisStreamOperations operations) {
        super(mqProperties);
        this.redisProperties = Objects.requireNonNull(redisProperties, "redisProperties");
        this.operations = operations;
        this.brokerAdapter = null;
    }

    public Ddd4jRedisStreamMqGuiceModule(RedisStreamMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jRedisStreamMqGuiceModule(RedisStreamMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.redisProperties = new RedisStreamMQProperties();
        this.operations = null;
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(RedisStreamMQProperties.class).toInstance(redisProperties);
    }

    @Provides
    @Singleton
    public RedisStreamMQBrokerAdapter redisStreamMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        RedisStreamOperations resolved = Objects.isNull(operations) ? redisProperties.newOperations() : operations;
        return new RedisStreamMQBrokerAdapter(redisProperties, mqProperties(), serialization(), resolved);
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(RedisStreamMQBrokerAdapter redisStreamMQBrokerAdapter) {
        return redisStreamMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(RedisStreamMQBrokerAdapter redisStreamMQBrokerAdapter) {
        return redisStreamMQBrokerAdapter.createPublisher(mqProperties());
    }
}
