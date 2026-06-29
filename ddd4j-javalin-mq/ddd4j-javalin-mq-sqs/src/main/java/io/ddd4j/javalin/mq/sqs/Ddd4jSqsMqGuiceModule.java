package io.ddd4j.javalin.mq.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.sqs.consumer.SqsMQConsumerEndpointRegistrar;
import io.ddd4j.mq.sqs.publisher.SqsMQEventPublisher;
import io.ddd4j.mq.sqs.spi.SqsMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ddd4j-javalin MQ - sqs（AWS SQS）的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-sqs 的 {@code Ddd4jSqsMQAutoConfiguration}（Spring 自动配置），
 * 将纯 Java 的 SQS broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link SqsMQConsumerEndpointRegistrar} —— SQS 消费端点注册器（长轮询）</li>
 *   <li>{@link SqsMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 SQS 实现）</li>
 * </ul>
 *
 * <p>与 Spring 版本的区别：{@link AmazonSQS}（AWS SDK 客户端）与 {@code defaultQueueUrl}
 * 由业务方显式提供并传入 Module，而非由 Spring 自动创建（javalin 环境下显式管理外部资源更清晰）。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 AmazonSQS 客户端
 * AmazonSQS sqs = AmazonSQSClientBuilder.standard()
 *         .withRegion("us-east-1")
 *         .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
 *         .build();
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jSqsMqGuiceModule(sqs, "https://sqs.us-east-1.amazonaws.com/123/my-queue")
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jSqsMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jSqsMqGuiceModule.class);

    private final AmazonSQS amazonSqs;
    private final String defaultQueueUrl;
    private final Ddd4jMQProperties mqProperties;

    /**
     * @param amazonSqs        AWS SQS 客户端（业务方创建）
     * @param defaultQueueUrl  默认队列 URL（如 {@code https://sqs.us-east-1.amazonaws.com/123/my-queue}）
     */
    public Ddd4jSqsMqGuiceModule(AmazonSQS amazonSqs, String defaultQueueUrl) {
        this(amazonSqs, defaultQueueUrl, new Ddd4jMQProperties());
    }

    /**
     * @param amazonSqs        AWS SQS 客户端
     * @param defaultQueueUrl  默认队列 URL
     * @param mqProperties     ddd4j MQ 通用配置
     */
    public Ddd4jSqsMqGuiceModule(AmazonSQS amazonSqs, String defaultQueueUrl,
                                 Ddd4jMQProperties mqProperties) {
        this.amazonSqs = amazonSqs;
        this.defaultQueueUrl = defaultQueueUrl;
        this.mqProperties = mqProperties;
    }

    @Override
    protected void configure() {
        bind(AmazonSQS.class).toInstance(amazonSqs);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 sqsMQConsumerEndpointRegistrar Bean）。
     *
     * <p>注册 JVM 停机钩子，在应用关闭时停止 SQS 长轮询任务。
     */
    @Provides
    @Singleton
    public SqsMQConsumerEndpointRegistrar sqsMQConsumerEndpointRegistrar() {
        SqsMQConsumerEndpointRegistrar registrar =
                new SqsMQConsumerEndpointRegistrar(amazonSqs, defaultQueueUrl, mqProperties);
        // 注册停机钩子（对标 Spring @Bean(destroyMethod = "close")）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing SqsMQConsumerEndpointRegistrar via JVM hook");
            registrar.close();
        }, "ddd4j-javalin-sqs-shutdown"));
        log.info("SqsMQConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 sqsMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter sqsMQBrokerAdapter(SqsMQConsumerEndpointRegistrar registrar) {
        return new SqsMQBrokerAdapter(amazonSqs, defaultQueueUrl, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 sqsMQEventPublisher Bean）。
     *
     * <p>绑定到 ddd4j-core 的 {@link MQEventPublisher}，让领域层通过统一契约发布事件。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new SqsMQEventPublisher(amazonSqs, defaultQueueUrl, mqProperties);
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
