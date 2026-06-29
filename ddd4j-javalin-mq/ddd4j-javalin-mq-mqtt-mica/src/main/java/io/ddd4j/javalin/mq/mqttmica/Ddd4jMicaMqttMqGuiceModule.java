package io.ddd4j.javalin.mq.mqttmica;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.mqtt.mica.config.Ddd4jMicaMqttProperties;
import io.ddd4j.mq.mqtt.mica.consumer.MicaMqttMQConsumerEndpointRegistrar;
import io.ddd4j.mq.mqtt.mica.publisher.MicaMqttMQEventPublisher;
import io.ddd4j.mq.mqtt.mica.spi.MicaMqttMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.dromara.mica.mqtt.spring.client.MqttClientTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ddd4j-javalin MQ - mqtt-mica 的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-mqtt-mica 的 {@code Ddd4jMicaMqttMQAutoConfiguration}（Spring 自动配置），
 * 将 mica-mqtt broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link MicaMqttMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 mica-mqtt 实现）</li>
 *   <li>{@link MicaMqttMQConsumerEndpointRegistrar} —— 消费端点注册器</li>
 * </ul>
 *
 * <p><b>架构说明</b>：ddd4j-mq-mqtt-mica 基于 mica-mqtt（org.dromara.mica.mqtt）的
 * {@link MqttClientTemplate}（AIO 客户端）。业务方提供 MqttClientTemplate（mica-mqtt 客户端），
 * Module 装配 ddd4j 组件。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 mica-mqtt 客户端（连接外部 Broker）
 * MqttClientTemplate mqttClientTemplate = ...;  // mica-mqtt 客户端
 * Ddd4jMicaMqttProperties micaProps = new Ddd4jMicaMqttProperties();
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jMicaMqttMqGuiceModule(mqttClientTemplate, micaProps)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jMicaMqttMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jMicaMqttMqGuiceModule.class);

    private final MqttClientTemplate mqttClientTemplate;
    private final Ddd4jMQProperties mqProperties;
    private final Ddd4jMicaMqttProperties micaMqttProperties;

    /**
     * @param mqttClientTemplate mica-mqtt 的 MqttClientTemplate（业务方创建）
     * @param micaMqttProperties mica-mqtt 配置（qos 等）
     */
    public Ddd4jMicaMqttMqGuiceModule(MqttClientTemplate mqttClientTemplate,
                                      Ddd4jMicaMqttProperties micaMqttProperties) {
        this(mqttClientTemplate, new Ddd4jMQProperties(), micaMqttProperties);
    }

    /**
     * @param mqttClientTemplate MqttClientTemplate
     * @param mqProperties       ddd4j MQ 通用配置
     * @param micaMqttProperties mica-mqtt 配置
     */
    public Ddd4jMicaMqttMqGuiceModule(MqttClientTemplate mqttClientTemplate,
                                      Ddd4jMQProperties mqProperties,
                                      Ddd4jMicaMqttProperties micaMqttProperties) {
        this.mqttClientTemplate = mqttClientTemplate;
        this.mqProperties = mqProperties;
        this.micaMqttProperties = micaMqttProperties;
    }

    @Override
    protected void configure() {
        bind(MqttClientTemplate.class).toInstance(mqttClientTemplate);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 micaMqttMQConsumerEndpointRegistrar Bean）。
     */
    @Provides
    @Singleton
    public MicaMqttMQConsumerEndpointRegistrar micaMqttMQConsumerEndpointRegistrar() {
        MicaMqttMQConsumerEndpointRegistrar registrar =
                new MicaMqttMQConsumerEndpointRegistrar(mqttClientTemplate, mqProperties, micaMqttProperties);
        log.info("MicaMqttMQConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 micaMqttMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter micaMqttMQBrokerAdapter(MicaMqttMQConsumerEndpointRegistrar registrar) {
        return new MicaMqttMQBrokerAdapter(mqttClientTemplate, mqProperties, micaMqttProperties.getQos(), registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 micaMqttMQEventPublisher Bean）。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new MicaMqttMQEventPublisher(mqttClientTemplate, mqProperties, micaMqttProperties.getQos());
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
