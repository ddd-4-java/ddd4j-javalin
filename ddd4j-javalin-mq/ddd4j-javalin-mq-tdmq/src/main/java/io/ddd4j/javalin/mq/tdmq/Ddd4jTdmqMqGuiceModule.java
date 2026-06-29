package io.ddd4j.javalin.mq.tdmq;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.ddd4j.mq.tdmq.client.TdmqClient;
import io.ddd4j.mq.tdmq.client.TdmqClientPlaceholder;
import io.ddd4j.mq.tdmq.consumer.TdmqMQConsumerEndpointRegistrar;
import io.ddd4j.mq.tdmq.publisher.TdmqMQEventPublisher;
import io.ddd4j.mq.tdmq.spi.TdmqMQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ddd4j-javalin MQ - tdmq（腾讯云 TDMQ）的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-tdmq 的 {@code Ddd4jTdmqMQAutoConfiguration}（Spring 自动配置），
 * 将 TDMQ broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link TdmqMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 TDMQ 实现）</li>
 *   <li>{@link TdmqMQConsumerEndpointRegistrar} —— 消费端点注册器</li>
 * </ul>
 *
 * <p><b>架构说明</b>：ddd4j-mq-tdmq 基于 ddd4j 自己的 {@link TdmqClient} 抽象接口
 * （纯 Java，零 Spring 依赖）。默认使用 {@link TdmqClientPlaceholder}，业务方可传入自定义实现。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 默认占位客户端（测试用）
 * Injector injector = Guice.createInjector(new Ddd4jTdmqMqGuiceModule());
 * // 或业务方提供真实客户端：
 * // Injector injector = Guice.createInjector(new Ddd4jTdmqMqGuiceModule(myTdmqClient));
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jTdmqMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jTdmqMqGuiceModule.class);

    private final TdmqClient tdmqClient;
    private final Ddd4jMQProperties mqProperties;

    /**
     * 默认构造（使用 TdmqClientPlaceholder 占位客户端）。
     */
    public Ddd4jTdmqMqGuiceModule() {
        this(new TdmqClientPlaceholder(), new Ddd4jMQProperties());
    }

    /**
     * @param tdmqClient 业务方提供的 TDMQ 客户端实现
     */
    public Ddd4jTdmqMqGuiceModule(TdmqClient tdmqClient) {
        this(tdmqClient, new Ddd4jMQProperties());
    }

    /**
     * @param tdmqClient   TDMQ 客户端
     * @param mqProperties ddd4j MQ 通用配置
     */
    public Ddd4jTdmqMqGuiceModule(TdmqClient tdmqClient, Ddd4jMQProperties mqProperties) {
        this.tdmqClient = tdmqClient;
        this.mqProperties = mqProperties;
    }

    @Override
    protected void configure() {
        bind(TdmqClient.class).toInstance(tdmqClient);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 tdmqMQConsumerEndpointRegistrar Bean）。
     */
    @Provides
    @Singleton
    public TdmqMQConsumerEndpointRegistrar tdmqMQConsumerEndpointRegistrar() {
        TdmqMQConsumerEndpointRegistrar registrar =
                new TdmqMQConsumerEndpointRegistrar(tdmqClient, mqProperties);
        log.info("TdmqMQConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 tdmqMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter tdmqMQBrokerAdapter(TdmqMQConsumerEndpointRegistrar registrar) {
        return new TdmqMQBrokerAdapter(tdmqClient, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 tdmqMQEventPublisher Bean）。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new TdmqMQEventPublisher(tdmqClient, mqProperties);
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
