package io.ddd4j.javalin.mq.kafka;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.kafka.mq.KafkaMQBrokerAdapter;
import io.ddd4j.mq.kafka.mq.KafkaMQEventPublisher;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.serialization.MQMessageSerialization;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * ddd4j-javalin MQ - kafka 的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-kafka 的 {@code Ddd4jKafkaMQAutoConfiguration}（Spring 自动配置），
 * 将 Kafka broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link KafkaMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 Kafka 实现）</li>
 *   <li>{@link MQMessageSerialization} —— 消息序列化器（默认 JSON 实现）</li>
 * </ul>
 *
 * <p><b>架构说明</b>：ddd4j-mq-kafka 的 BrokerAdapter 基于 spring-kafka 的
 * {@link KafkaTemplate} / {@link ConsumerFactory}（这些是 Spring Kafka 的核心 API，但本身
 * 可通过纯 Java 构造，如 {@code new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configs))}，
 * 不依赖 Spring 容器）。业务方提供这些对象，Module 装配 ddd4j 组件。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 spring-kafka 客户端（纯 Java 构造，无需 Spring 容器）
 * ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerConfigs);
 * KafkaTemplate<String, String> template = new KafkaTemplate<>(pf);
 * ConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerConfigs);
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jKafkaMqGuiceModule(template, cf)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jKafkaMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jKafkaMqGuiceModule.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;
    private final Ddd4jMQProperties mqProperties;
    private final MQMessageSerialization serialization;

    /**
     * @param kafkaTemplate   spring-kafka 的 KafkaTemplate（业务方创建）
     * @param consumerFactory spring-kafka 的 ConsumerFactory（业务方创建）
     */
    public Ddd4jKafkaMqGuiceModule(KafkaTemplate<String, String> kafkaTemplate,
                                   ConsumerFactory<String, String> consumerFactory) {
        this(kafkaTemplate, consumerFactory, new Ddd4jMQProperties());
    }

    /**
     * @param kafkaTemplate   spring-kafka 的 KafkaTemplate
     * @param consumerFactory spring-kafka 的 ConsumerFactory
     * @param mqProperties    ddd4j MQ 通用配置
     */
    public Ddd4jKafkaMqGuiceModule(KafkaTemplate<String, String> kafkaTemplate,
                                   ConsumerFactory<String, String> consumerFactory,
                                   Ddd4jMQProperties mqProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
        this.mqProperties = mqProperties;
        this.serialization = new io.ddd4j.mq.serialization.JsonMQMessageSerialization();
    }

    @Override
    protected void configure() {
        bind(KafkaTemplate.class).toInstance(kafkaTemplate);
    }

    /**
     * 提供消息序列化器（对标 Spring 容器中的 MQMessageSerialization Bean）。
     *
     * <p>默认使用 {@link io.ddd4j.mq.serialization.JsonMQMessageSerialization}（纯 Java，JSON 序列化）。
     */
    @Provides
    @Singleton
    public MQMessageSerialization mqMessageSerialization() {
        return serialization;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 kafkaMQBrokerAdapter Bean）。
     *
     * <p>注册 JVM 停机钩子，在应用关闭时停止所有 Kafka 消费容器
     * （对标 Spring 的 DisposableBean.destroy()）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter kafkaMQBrokerAdapter() {
        KafkaMQBrokerAdapter adapter = new KafkaMQBrokerAdapter(kafkaTemplate, consumerFactory, serialization);
        // 注册停机钩子（对标 Spring DisposableBean.destroy）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Destroying KafkaMQBrokerAdapter via JVM hook");
            adapter.destroy();
        }, "ddd4j-javalin-kafka-shutdown"));
        log.info("KafkaMQBrokerAdapter initialized");
        return adapter;
    }

    /**
     * 提供事件发布器（对标 Spring 的 kafkaMQEventPublisher Bean）。
     *
     * <p>绑定到 ddd4j-core 的 {@link MQEventPublisher}，让领域层通过统一契约发布事件。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new KafkaMQEventPublisher(kafkaTemplate, serialization, mqProperties);
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
