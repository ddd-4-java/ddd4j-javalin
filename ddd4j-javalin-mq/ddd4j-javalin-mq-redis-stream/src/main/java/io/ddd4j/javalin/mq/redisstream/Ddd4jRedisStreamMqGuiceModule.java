package io.ddd4j.javalin.mq.redisstream;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.redisstream.consumer.RedisStreamConsumerEndpointRegistrar;
import io.ddd4j.mq.redisstream.publisher.RedisStreamMQEventPublisher;
import io.ddd4j.mq.redisstream.spi.RedisStreamMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * ddd4j-javalin MQ - redis-stream 的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-redis-stream 的 {@code Ddd4jRedisStreamMQAutoConfiguration}（Spring 自动配置），
 * 将 Redis Stream broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link RedisStreamMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 Redis Stream 实现）</li>
 *   <li>{@link RedisStreamConsumerEndpointRegistrar} —— 消费端点注册器</li>
 * </ul>
 *
 * <p><b>架构说明</b>：ddd4j-mq-redis-stream 基于 spring-data-redis 的 {@link StringRedisTemplate}。
 * 其 {@link RedisStreamConsumerEndpointRegistrar} 需要 Spring {@link ApplicationContext} 与
 * {@link StringRedisTemplate}。本 Module 提供轻量 {@code StaticApplicationContext} 适配器，
 * 并将业务方提供的 StringRedisTemplate 传入 registrar。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 StringRedisTemplate（spring-data-redis）
 * StringRedisTemplate redisTemplate = new StringRedisTemplate(lettuceConnectionFactory);
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jRedisStreamMqGuiceModule(redisTemplate)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jRedisStreamMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jRedisStreamMqGuiceModule.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final Ddd4jMQProperties mqProperties;
    private final ApplicationContext applicationContext;

    /**
     * @param stringRedisTemplate spring-data-redis 的 StringRedisTemplate（业务方创建）
     */
    public Ddd4jRedisStreamMqGuiceModule(StringRedisTemplate stringRedisTemplate) {
        this(stringRedisTemplate, new Ddd4jMQProperties());
    }

    /**
     * @param stringRedisTemplate StringRedisTemplate
     * @param mqProperties        ddd4j MQ 通用配置
     */
    public Ddd4jRedisStreamMqGuiceModule(StringRedisTemplate stringRedisTemplate, Ddd4jMQProperties mqProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.mqProperties = mqProperties;
        // 轻量 ApplicationContext（满足 registrar 对 ApplicationContext 的最小依赖）
        this.applicationContext = new org.springframework.context.support.StaticApplicationContext();
    }

    @Override
    protected void configure() {
        bind(StringRedisTemplate.class).toInstance(stringRedisTemplate);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 redisStreamConsumerEndpointRegistrar Bean）。
     */
    @Provides
    @Singleton
    public RedisStreamConsumerEndpointRegistrar redisStreamConsumerEndpointRegistrar() {
        RedisStreamConsumerEndpointRegistrar registrar =
                new RedisStreamConsumerEndpointRegistrar(applicationContext, mqProperties, stringRedisTemplate);
        log.info("RedisStreamConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 redisStreamMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter redisStreamMQBrokerAdapter(RedisStreamConsumerEndpointRegistrar registrar) {
        return new RedisStreamMQBrokerAdapter(stringRedisTemplate, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 redisStreamMQEventPublisher Bean）。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new RedisStreamMQEventPublisher(stringRedisTemplate, mqProperties);
    }

    /**
     * 暴露 ddd4j MQ 通用配置。
     */
    @Provides
    @Singleton
    public Ddd4jMQProperties ddd4jMQProperties() {
        return mqProperties;
    }
}
