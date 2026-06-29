package io.ddd4j.javalin.mq.mqtt;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.mqtt.config.Ddd4jMqttProperties;
import io.ddd4j.mq.mqtt.consumer.MqttMQConsumerEndpointRegistrar;
import io.ddd4j.mq.mqtt.publisher.MqttMQEventPublisher;
import io.ddd4j.mq.mqtt.spi.MqttMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.messaging.MessageChannel;

/**
 * ddd4j-javalin MQ - mqtt 的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-mqtt 的 {@code Ddd4jMqttMQAutoConfiguration}（Spring 自动配置），
 * 将 MQTT broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link MqttMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 MQTT 实现）</li>
 *   <li>{@link MqttMQConsumerEndpointRegistrar} —— 消费端点注册器</li>
 * </ul>
 *
 * <p><b>架构说明</b>：ddd4j-mq-mqtt 基于 Eclipse Paho + Spring Integration。
 * 业务方需提供 {@link MessageChannel}（出站通道，如 {@code DirectChannel}）+
 * {@link MqttPahoClientFactory}（Paho 客户端工厂），Module 装配 ddd4j 组件。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 MQTT 基础设施（spring-integration-mqtt）
 * MessageChannel outboundChannel = new DirectChannel();
 * MqttPahoClientFactory clientFactory = ...;  // 配置 broker url/credentials
 * Ddd4jMqttProperties mqttProps = new Ddd4jMqttProperties();  // qos 等
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jMqttMqGuiceModule(outboundChannel, clientFactory, mqttProps)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jMqttMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jMqttMqGuiceModule.class);

    private final MessageChannel mqttOutboundChannel;
    private final MqttPahoClientFactory mqttClientFactory;
    private final Ddd4jMQProperties mqProperties;
    private final Ddd4jMqttProperties mqttProperties;

    /**
     * @param mqttOutboundChannel spring-messaging 出站通道（业务方创建，如 DirectChannel）
     * @param mqttClientFactory   spring-integration-mqtt 的 Paho 客户端工厂（业务方创建）
     * @param mqttProperties      MQTT 配置（qos 等）
     */
    public Ddd4jMqttMqGuiceModule(MessageChannel mqttOutboundChannel,
                                  MqttPahoClientFactory mqttClientFactory,
                                  Ddd4jMqttProperties mqttProperties) {
        this(mqttOutboundChannel, mqttClientFactory, new Ddd4jMQProperties(), mqttProperties);
    }

    /**
     * @param mqttOutboundChannel 出站通道
     * @param mqttClientFactory   Paho 客户端工厂
     * @param mqProperties        ddd4j MQ 通用配置
     * @param mqttProperties      MQTT 配置
     */
    public Ddd4jMqttMqGuiceModule(MessageChannel mqttOutboundChannel,
                                  MqttPahoClientFactory mqttClientFactory,
                                  Ddd4jMQProperties mqProperties,
                                  Ddd4jMqttProperties mqttProperties) {
        this.mqttOutboundChannel = mqttOutboundChannel;
        this.mqttClientFactory = mqttClientFactory;
        this.mqProperties = mqProperties;
        this.mqttProperties = mqttProperties;
    }

    @Override
    protected void configure() {
        bind(MessageChannel.class).toInstance(mqttOutboundChannel);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 mqttMQConsumerEndpointRegistrar Bean）。
     */
    @Provides
    @Singleton
    public MqttMQConsumerEndpointRegistrar mqttMQConsumerEndpointRegistrar() {
        MqttMQConsumerEndpointRegistrar registrar =
                new MqttMQConsumerEndpointRegistrar(mqttClientFactory, mqProperties, mqttProperties);
        log.info("MqttMQConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 mqttMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter mqttMQBrokerAdapter(MqttMQConsumerEndpointRegistrar registrar) {
        return new MqttMQBrokerAdapter(mqttOutboundChannel, mqProperties, mqttProperties.getQos(), registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 mqttMQEventPublisher Bean）。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new MqttMQEventPublisher(mqttOutboundChannel, mqProperties, mqttProperties.getQos());
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
