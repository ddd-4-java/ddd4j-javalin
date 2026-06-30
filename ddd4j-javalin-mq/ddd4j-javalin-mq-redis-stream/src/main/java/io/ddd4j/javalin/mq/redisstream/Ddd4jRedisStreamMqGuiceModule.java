package io.ddd4j.javalin.mq.redisstream;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.redisstream.consumer.RedisStreamConsumerEndpointRegistrar;
import io.ddd4j.mq.redisstream.publisher.RedisStreamMQEventPublisher;
import io.ddd4j.mq.redisstream.spi.RedisStreamMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.redisstream.Ddd4jRedisStreamMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jRedisStreamMqGuiceModule extends io.ddd4j.guice.mq.redisstream.Ddd4jRedisStreamMqGuiceModule {

    public Ddd4jRedisStreamMqGuiceModule(StringRedisTemplate stringRedisTemplate) {
        super(stringRedisTemplate);
    }

    public Ddd4jRedisStreamMqGuiceModule(StringRedisTemplate stringRedisTemplate, Ddd4jMQProperties mqProperties) {
        super(stringRedisTemplate, mqProperties);
    }

}
