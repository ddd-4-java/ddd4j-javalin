package io.ddd4j.javalin.mq.rocket;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.rocket.consumer.RocketMQConsumerEndpointRegistrar;
import io.ddd4j.mq.rocket.publisher.RocketMQEventPublisher;
import io.ddd4j.mq.rocket.spi.RocketMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * ddd4j-javalin MQ - rocketmq 的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-rocketmq 的 {@code Ddd4jRocketMQAutoConfiguration}（Spring 自动配置），
 * 将 RocketMQ broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link RocketMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 RocketMQ 实现）</li>
 *   <li>{@link RocketMQConsumerEndpointRegistrar} —— 消费端点注册器</li>
 * </ul>
 *
 * <p><b>架构说明</b>：ddd4j-mq-rocketmq 基于 rocketmq-spring-boot-starter 的 {@link RocketMQTemplate}。
 * 其 {@link RocketMQConsumerEndpointRegistrar} 需要 Spring {@link ApplicationContext} 来获取
 * {@link RocketMQProperties}。本 Module 提供 {@link LightweightApplicationContext} 适配器
 * （仅持有必要 Bean），让 registrar 在 Guice 环境下正常工作。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 RocketMQTemplate（rocketmq-spring，可纯 Java 构造）
 * RocketMQTemplate template = new RocketMQTemplate(producer);
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jRocketMqGuiceModule(template)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jRocketMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jRocketMqGuiceModule.class);

    private final RocketMQTemplate rocketMQTemplate;
    private final Ddd4jMQProperties mqProperties;
    private final RocketMQProperties rocketMQProperties;
    private final ApplicationContext applicationContext;

    /**
     * @param rocketMQTemplate rocketmq-spring 的 RocketMQTemplate（业务方创建）
     */
    public Ddd4jRocketMqGuiceModule(RocketMQTemplate rocketMQTemplate) {
        this(rocketMQTemplate, new Ddd4jMQProperties(), new RocketMQProperties());
    }

    /**
     * @param rocketMQTemplate    RocketMQTemplate
     * @param mqProperties        ddd4j MQ 通用配置
     * @param rocketMQProperties  rocketmq-spring 配置（ConsumerRegistrar 通过 ApplicationContext 获取）
     */
    public Ddd4jRocketMqGuiceModule(RocketMQTemplate rocketMQTemplate,
                                    Ddd4jMQProperties mqProperties,
                                    RocketMQProperties rocketMQProperties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.mqProperties = mqProperties;
        this.rocketMQProperties = rocketMQProperties;
        // 构造轻量 ApplicationContext（仅暴露 RocketMQProperties，供 ConsumerRegistrar 使用）
        this.applicationContext = new LightweightApplicationContext(rocketMQProperties);
    }

    @Override
    protected void configure() {
        bind(RocketMQTemplate.class).toInstance(rocketMQTemplate);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 rocketMQConsumerEndpointRegistrar Bean）。
     *
     * <p>使用 {@link LightweightApplicationContext} 桥接 Spring {@link ApplicationContext} 依赖。
     * 注册 JVM 停机钩子，在应用关闭时清理 RocketMQ Consumer。
     */
    @Provides
    @Singleton
    public RocketMQConsumerEndpointRegistrar rocketMQConsumerEndpointRegistrar() {
        RocketMQConsumerEndpointRegistrar registrar =
                new RocketMQConsumerEndpointRegistrar(applicationContext, mqProperties);
        // 注册停机钩子（对标 Spring @Bean(destroyMethod = "close")）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing RocketMQConsumerEndpointRegistrar via JVM hook");
            try {
                registrar.close();
            } catch (Exception e) {
                log.warn("Failed to close RocketMQConsumerEndpointRegistrar", e);
            }
        }, "ddd4j-javalin-rocketmq-shutdown"));
        log.info("RocketMQConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 rocketMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter rocketMQBrokerAdapter(RocketMQConsumerEndpointRegistrar registrar) {
        return new RocketMQBrokerAdapter(rocketMQTemplate, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 rocketMQEventPublisher Bean）。
     *
     * <p>绑定到 ddd4j-core 的 {@link MQEventPublisher}，让领域层通过统一契约发布事件。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new RocketMQEventPublisher(rocketMQTemplate, mqProperties);
    }

    /**
     * 暴露 ddd4j MQ 通用配置。
     */
    @Provides
    @Singleton
    public Ddd4jMQProperties ddd4jMQProperties() {
        return mqProperties;
    }

    /**
     * 轻量级 Spring {@link ApplicationContext} 适配器。
     *
     * <p>Guice 环境下无真实 ApplicationContext，但 {@link RocketMQConsumerEndpointRegistrar}
     * 需要通过 {@code applicationContext.getBean(RocketMQProperties.class)} 获取配置。
     * 本适配器仅持有必要的 Bean，满足 registrar 的最小依赖。
     */
    static class LightweightApplicationContext
            extends org.springframework.context.support.StaticApplicationContext {
        LightweightApplicationContext(RocketMQProperties rocketMQProperties) {
            // 注册 RocketMQProperties 为单例 Bean
            getBeanFactory().registerSingleton("rocketMQProperties", rocketMQProperties);
        }
    }
}
