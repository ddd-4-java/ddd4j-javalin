package io.ddd4j.javalin.mq.rabbit;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.rabbit.consumer.RabbitMQConsumerEndpointRegistrar;
import io.ddd4j.mq.rabbit.publisher.RabbitMQEventPublisher;
import io.ddd4j.mq.rabbit.spi.RabbitMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.context.ApplicationContext;

/**
 * ddd4j-javalin MQ - rabbitmq 的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-rabbitmq 的 {@code Ddd4jRabbitMQAutoConfiguration}（Spring 自动配置），
 * 将 RabbitMQ broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link RabbitMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 RabbitMQ 实现）</li>
 *   <li>{@link RabbitMQConsumerEndpointRegistrar} —— 消费端点注册器</li>
 * </ul>
 *
 * <p><b>架构说明</b>：ddd4j-mq-rabbitmq 基于 spring-amqp 的 {@link RabbitTemplate}。
 * 其 {@link RabbitMQConsumerEndpointRegistrar} 需要 Spring {@link ApplicationContext} 与
 * {@link RabbitListenerEndpointRegistry}。本 Module 提供轻量 ApplicationContext 适配器 +
 * 纯 Java 创建的 RabbitListenerEndpointRegistry，让 registrar 在 Guice 环境下正常工作。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 RabbitTemplate（spring-amqp，可纯 Java 构造）
 * RabbitTemplate template = new RabbitTemplate(connectionFactory);
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jRabbitMqGuiceModule(template)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jRabbitMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jRabbitMqGuiceModule.class);

    private final RabbitTemplate rabbitTemplate;
    private final Ddd4jMQProperties mqProperties;
    private final RabbitListenerEndpointRegistry endpointRegistry;
    private final ApplicationContext applicationContext;

    /**
     * @param rabbitTemplate spring-amqp 的 RabbitTemplate（业务方创建）
     */
    public Ddd4jRabbitMqGuiceModule(RabbitTemplate rabbitTemplate) {
        this(rabbitTemplate, new Ddd4jMQProperties());
    }

    /**
     * @param rabbitTemplate RabbitTemplate
     * @param mqProperties   ddd4j MQ 通用配置
     */
    public Ddd4jRabbitMqGuiceModule(RabbitTemplate rabbitTemplate, Ddd4jMQProperties mqProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.mqProperties = mqProperties;
        // RabbitListenerEndpointRegistry 可纯 Java 创建（spring-amqp 标准组件）
        this.endpointRegistry = new RabbitListenerEndpointRegistry();
        // 轻量 ApplicationContext（满足 registrar 对 ApplicationContext 的最小依赖）
        this.applicationContext = new org.springframework.context.support.StaticApplicationContext();
    }

    @Override
    protected void configure() {
        bind(RabbitTemplate.class).toInstance(rabbitTemplate);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 rabbitMQConsumerEndpointRegistrar Bean）。
     *
     * <p>使用轻量 {@link ApplicationContext} + 纯 Java {@link RabbitListenerEndpointRegistry}
     * 桥接 Spring 依赖。
     */
    @Provides
    @Singleton
    public RabbitMQConsumerEndpointRegistrar rabbitMQConsumerEndpointRegistrar() {
        RabbitMQConsumerEndpointRegistrar registrar =
                new RabbitMQConsumerEndpointRegistrar(applicationContext, endpointRegistry, mqProperties);
        log.info("RabbitMQConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 rabbitMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter rabbitMQBrokerAdapter(RabbitMQConsumerEndpointRegistrar registrar) {
        return new RabbitMQBrokerAdapter(rabbitTemplate, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 rabbitMQEventPublisher Bean）。
     *
     * <p>绑定到 ddd4j-core 的 {@link MQEventPublisher}，让领域层通过统一契约发布事件。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new RabbitMQEventPublisher(rabbitTemplate, mqProperties);
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
