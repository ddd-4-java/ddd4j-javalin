package io.ddd4j.javalin.mq.activemq;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.activemq.consumer.ActiveMQConsumerEndpointRegistrar;
import io.ddd4j.mq.activemq.publisher.ActiveMQEventPublisher;
import io.ddd4j.mq.activemq.spi.ActiveMQBrokerAdapter;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.core.JmsTemplate;

/**
 * ddd4j-javalin MQ - activemq（Artemis）的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-activemq 的 {@code Ddd4jActiveMQAutoConfiguration}（Spring 自动配置），
 * 将 ActiveMQ broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link ActiveMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 ActiveMQ 实现）</li>
 *   <li>{@link ActiveMQConsumerEndpointRegistrar} —— 消费端点注册器</li>
 * </ul>
 *
 * <p><b>架构说明</b>：ddd4j-mq-activemq 基于 spring-jms 的 {@link JmsTemplate}。
 * 其 {@link ActiveMQConsumerEndpointRegistrar} 需要 Spring {@link ApplicationContext} 与
 * {@link JmsListenerEndpointRegistry}。本 Module 提供轻量 ApplicationContext 适配器 +
 * 纯 Java 创建的 JmsListenerEndpointRegistry，让 registrar 在 Guice 环境下正常工作。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 JmsTemplate（spring-jms，可纯 Java 构造）
 * JmsTemplate template = new JmsTemplate(connectionFactory);
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jActiveMqGuiceModule(template)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jActiveMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jActiveMqGuiceModule.class);

    private final JmsTemplate jmsTemplate;
    private final Ddd4jMQProperties mqProperties;
    private final JmsListenerEndpointRegistry endpointRegistry;
    private final ApplicationContext applicationContext;

    /**
     * @param jmsTemplate spring-jms 的 JmsTemplate（业务方创建）
     */
    public Ddd4jActiveMqGuiceModule(JmsTemplate jmsTemplate) {
        this(jmsTemplate, new Ddd4jMQProperties());
    }

    /**
     * @param jmsTemplate  JmsTemplate
     * @param mqProperties ddd4j MQ 通用配置
     */
    public Ddd4jActiveMqGuiceModule(JmsTemplate jmsTemplate, Ddd4jMQProperties mqProperties) {
        this.jmsTemplate = jmsTemplate;
        this.mqProperties = mqProperties;
        // JmsListenerEndpointRegistry 可纯 Java 创建（spring-jms 标准组件）
        this.endpointRegistry = new JmsListenerEndpointRegistry();
        // 轻量 ApplicationContext（满足 registrar 对 ApplicationContext 的最小依赖）
        this.applicationContext = new org.springframework.context.support.StaticApplicationContext();
    }

    @Override
    protected void configure() {
        bind(JmsTemplate.class).toInstance(jmsTemplate);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 activeMQConsumerEndpointRegistrar Bean）。
     */
    @Provides
    @Singleton
    public ActiveMQConsumerEndpointRegistrar activeMQConsumerEndpointRegistrar() {
        ActiveMQConsumerEndpointRegistrar registrar =
                new ActiveMQConsumerEndpointRegistrar(applicationContext, endpointRegistry, mqProperties);
        log.info("ActiveMQConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 activeMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter activeMQBrokerAdapter(ActiveMQConsumerEndpointRegistrar registrar) {
        return new ActiveMQBrokerAdapter(jmsTemplate, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 activeMQEventPublisher Bean）。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new ActiveMQEventPublisher(jmsTemplate, mqProperties);
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
