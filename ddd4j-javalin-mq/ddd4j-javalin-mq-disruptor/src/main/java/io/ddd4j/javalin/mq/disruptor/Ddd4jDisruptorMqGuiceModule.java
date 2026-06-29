package io.ddd4j.javalin.mq.disruptor;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.disruptor.config.DisruptorMQProperties;
import io.ddd4j.mq.disruptor.consumer.DisruptorMQConsumerEndpointRegistrar;
import io.ddd4j.mq.disruptor.core.DisruptorMQBus;
import io.ddd4j.mq.disruptor.core.DisruptorMQEventDispatcher;
import io.ddd4j.mq.disruptor.spi.DisruptorMQBrokerAdapter;
import io.ddd4j.mq.disruptor.publisher.DisruptorMQEventPublisher;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ddd4j-javalin MQ - disruptor 的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-disruptor 的 {@code Ddd4jDisruptorMQAutoConfiguration}（Spring 自动配置），
 * 将纯 Java 的 disruptor broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link DisruptorMQEventDispatcher} —— 事件分发器</li>
 *   <li>{@link DisruptorMQBus} —— RingBuffer 生命周期（单例，启动时构造，停机时 shutdown）</li>
 *   <li>{@link DisruptorMQConsumerEndpointRegistrar} —— 消费端点注册器</li>
 *   <li>{@link DisruptorMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 disruptor 实现）</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>{@code
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),                                  // 核心基础设施
 *     new Ddd4jDisruptorMqGuiceModule()                        // disruptor MQ
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jDisruptorMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jDisruptorMqGuiceModule.class);

    private final DisruptorMQProperties disruptorProperties;
    private final Ddd4jMQProperties mqProperties;

    /**
     * 默认构造（使用 disruptor 默认配置：bufferSize=1024，yielding 等待策略）。
     */
    public Ddd4jDisruptorMqGuiceModule() {
        this(new DisruptorMQProperties(), new Ddd4jMQProperties());
    }

    /**
     * 自定义配置构造。
     *
     * @param disruptorProperties disruptor 特定配置
     * @param mqProperties        ddd4j MQ 通用配置
     */
    public Ddd4jDisruptorMqGuiceModule(DisruptorMQProperties disruptorProperties,
                                       Ddd4jMQProperties mqProperties) {
        this.disruptorProperties = disruptorProperties;
        this.mqProperties = mqProperties;
    }

    @Override
    protected void configure() {
        // 绑定 Broker 适配 SPI（Guice 通过 @Provides 提供具体实现，此处仅声明绑定意向）
        bind(DisruptorMQEventDispatcher.class).in(Singleton.class);
    }

    /**
     * 提供 DisruptorMQBus（单例，启动时构造 RingBuffer）。
     *
     * <p>对标 Spring 的 {@code @Bean(destroyMethod = "shutdown")}。
     * Guice 无 destroyMethod，注册 JVM 关闭钩子保证 RingBuffer 正确停机。
     */
    @Provides
    @Singleton
    public DisruptorMQBus disruptorMQBus(DisruptorMQEventDispatcher dispatcher) {
        DisruptorMQBus bus = new DisruptorMQBus(disruptorProperties, dispatcher);
        // 注册停机钩子（替代 Spring @Bean(destroyMethod)）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down DisruptorMQBus via JVM hook");
            bus.shutdown();
        }, "ddd4j-javalin-disruptor-shutdown"));
        log.info("DisruptorMQBus initialized: bufferSize={}, waitStrategy={}",
                disruptorProperties.getBufferSize(), disruptorProperties.getWaitStrategy());
        return bus;
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 disruptorMQConsumerEndpointRegistrar Bean）。
     */
    @Provides
    @Singleton
    public DisruptorMQConsumerEndpointRegistrar disruptorMQConsumerEndpointRegistrar(DisruptorMQBus bus) {
        return new DisruptorMQConsumerEndpointRegistrar(bus);
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 disruptorMQBrokerAdapter Bean）。
     *
     * <p>绑定到通用 {@link MQBrokerAdapter}，让上层通过 SPI 统一访问。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter disruptorMQBrokerAdapter(
            DisruptorMQBus bus,
            DisruptorMQConsumerEndpointRegistrar registrar) {
        return new DisruptorMQBrokerAdapter(bus, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 disruptorMQEventPublisher Bean）。
     *
     * <p>绑定到 ddd4j-core 的 {@link MQEventPublisher}，让领域层通过统一契约发布事件。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(DisruptorMQBus bus) {
        return new DisruptorMQEventPublisher(bus, mqProperties);
    }

    /**
     * 暴露 disruptor 特定配置（供需要直接访问配置的组件注入）。
     */
    @Provides
    @Singleton
    public DisruptorMQProperties disruptorMQProperties() {
        return disruptorProperties;
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
