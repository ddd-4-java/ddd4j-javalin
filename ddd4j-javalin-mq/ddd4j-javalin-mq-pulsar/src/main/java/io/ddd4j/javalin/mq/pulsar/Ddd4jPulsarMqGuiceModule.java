package io.ddd4j.javalin.mq.pulsar;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.pulsar.consumer.PulsarConsumerEndpointRegistrar;
import io.ddd4j.mq.pulsar.publisher.PulsarMQEventPublisher;
import io.ddd4j.mq.pulsar.spi.PulsarMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.pulsar.core.PulsarTemplate;

/**
 * ddd4j-javalin MQ - pulsar 的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-pulsar 的 {@code Ddd4jPulsarMQAutoConfiguration}（Spring 自动配置），
 * 将 Pulsar broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link PulsarMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 Pulsar 实现）</li>
 *   <li>{@link PulsarConsumerEndpointRegistrar} —— 消费端点注册器</li>
 * </ul>
 *
 * <p><b>架构说明</b>：ddd4j-mq-pulsar 基于 spring-pulsar 的 {@link PulsarTemplate}。
 * 其 {@link PulsarConsumerEndpointRegistrar} 需要 Spring {@link ApplicationContext}。
 * 本 Module 提供轻量 {@code StaticApplicationContext} 适配器，让 registrar 在 Guice 环境下正常工作。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 PulsarTemplate（spring-pulsar）
 * PulsarTemplate<String> template = new PulsarTemplate<>(pulsarProducerFactory);
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jPulsarMqGuiceModule(template)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jPulsarMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jPulsarMqGuiceModule.class);

    private final PulsarTemplate<String> pulsarTemplate;
    private final Ddd4jMQProperties mqProperties;
    private final ApplicationContext applicationContext;

    /**
     * @param pulsarTemplate spring-pulsar 的 PulsarTemplate（业务方创建）
     */
    public Ddd4jPulsarMqGuiceModule(PulsarTemplate<String> pulsarTemplate) {
        this(pulsarTemplate, new Ddd4jMQProperties());
    }

    /**
     * @param pulsarTemplate PulsarTemplate
     * @param mqProperties   ddd4j MQ 通用配置
     */
    public Ddd4jPulsarMqGuiceModule(PulsarTemplate<String> pulsarTemplate, Ddd4jMQProperties mqProperties) {
        this.pulsarTemplate = pulsarTemplate;
        this.mqProperties = mqProperties;
        // 轻量 ApplicationContext（满足 registrar 对 ApplicationContext 的最小依赖）
        this.applicationContext = new org.springframework.context.support.StaticApplicationContext();
    }

    @Override
    protected void configure() {
        bind(PulsarTemplate.class).toInstance(pulsarTemplate);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 pulsarConsumerEndpointRegistrar Bean）。
     */
    @Provides
    @Singleton
    public PulsarConsumerEndpointRegistrar pulsarConsumerEndpointRegistrar() {
        PulsarConsumerEndpointRegistrar registrar =
                new PulsarConsumerEndpointRegistrar(applicationContext, mqProperties);
        log.info("PulsarConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 pulsarMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter pulsarMQBrokerAdapter(PulsarConsumerEndpointRegistrar registrar) {
        return new PulsarMQBrokerAdapter(pulsarTemplate, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 pulsarMQEventPublisher Bean）。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new PulsarMQEventPublisher(pulsarTemplate, mqProperties);
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
