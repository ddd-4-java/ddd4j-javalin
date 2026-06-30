package io.ddd4j.javalin.mq.redisstream;

import io.ddd4j.mq.config.Ddd4jMQProperties;
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
