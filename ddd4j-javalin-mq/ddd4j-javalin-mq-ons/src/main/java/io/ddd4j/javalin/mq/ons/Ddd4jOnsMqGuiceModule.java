package io.ddd4j.javalin.mq.ons;

import com.aliyun.openservices.ons.api.Producer;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.ons.consumer.OnsMQConsumerEndpointRegistrar;
import io.ddd4j.mq.ons.publisher.OnsMQEventPublisher;
import io.ddd4j.mq.ons.spi.OnsMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * ddd4j-javalin MQ - ons（阿里云 RocketMQ）的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-ons 的 {@code Ddd4jOnsMQAutoConfiguration}（Spring 自动配置），
 * 将纯 Java 的 ONS broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link OnsMQConsumerEndpointRegistrar} —— ONS 消费端点注册器</li>
 *   <li>{@link OnsMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 ONS 实现）</li>
 * </ul>
 *
 * <p>与 Spring 版本的区别：{@link Producer}（ONS 生产者）与连接 {@link Properties}
 * 由业务方显式提供并传入 Module，而非由 Spring 自动创建（javalin 环境下显式管理外部资源更清晰）。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 ONS 连接属性 + Producer
 * Properties props = new Properties();
 * props.setProperty(PropertyKeyConst.AccessKey, "xxx");
 * props.setProperty(PropertyKeyConst.SecretKey, "xxx");
 * props.setProperty(PropertyKeyConst.NAMESRV_ADDR, "xxx");
 * Producer producer = ONSFactory.createProducer(props);
 * producer.start();
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jOnsMqGuiceModule(producer, props)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jOnsMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jOnsMqGuiceModule.class);

    private final Producer producer;
    private final Properties onsConnectionProperties;
    private final Ddd4jMQProperties mqProperties;

    /**
     * @param producer               ONS 生产者（业务方创建并 start）
     * @param onsConnectionProperties ONS 连接属性（accessKey/secretKey/namesrv，Producer/Consumer 共用）
     */
    public Ddd4jOnsMqGuiceModule(Producer producer, Properties onsConnectionProperties) {
        this(producer, onsConnectionProperties, new Ddd4jMQProperties());
    }

    /**
     * @param producer               ONS 生产者
     * @param onsConnectionProperties ONS 连接属性
     * @param mqProperties           ddd4j MQ 通用配置
     */
    public Ddd4jOnsMqGuiceModule(Producer producer, Properties onsConnectionProperties,
                                 Ddd4jMQProperties mqProperties) {
        this.producer = producer;
        this.onsConnectionProperties = onsConnectionProperties;
        this.mqProperties = mqProperties;
    }

    @Override
    protected void configure() {
        bind(Producer.class).toInstance(producer);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 onsMQConsumerEndpointRegistrar Bean）。
     *
     * <p>注册 JVM 停机钩子，在应用关闭时清理 ONS Consumer。
     */
    @Provides
    @Singleton
    public OnsMQConsumerEndpointRegistrar onsMQConsumerEndpointRegistrar() {
        OnsMQConsumerEndpointRegistrar registrar =
                new OnsMQConsumerEndpointRegistrar(onsConnectionProperties, mqProperties);
        // 注册停机钩子（对标 Spring @Bean(destroyMethod = "close")）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing OnsMQConsumerEndpointRegistrar via JVM hook");
            registrar.close();
        }, "ddd4j-javalin-ons-shutdown"));
        log.info("OnsMQConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 onsMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter onsMQBrokerAdapter(OnsMQConsumerEndpointRegistrar registrar) {
        return new OnsMQBrokerAdapter(producer, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 onsMQEventPublisher Bean）。
     *
     * <p>绑定到 ddd4j-core 的 {@link MQEventPublisher}，让领域层通过统一契约发布事件。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new OnsMQEventPublisher(producer, mqProperties);
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
