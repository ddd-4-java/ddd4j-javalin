package io.ddd4j.javalin.mq.nats;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.nats.consumer.NatsMQConsumerEndpointRegistrar;
import io.ddd4j.mq.nats.publisher.NatsMQEventPublisher;
import io.ddd4j.mq.nats.spi.NatsMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.nats.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ddd4j-javalin MQ - nats 的 Guice 集成模块。
 *
 * <p>对标 ddd4j-mq-nats 的 {@code Ddd4jNatsMQAutoConfiguration}（Spring 自动配置），
 * 将纯 Java 的 NATS broker 组件装配到 Guice 容器：
 * <ul>
 *   <li>{@link NatsMQConsumerEndpointRegistrar} —— JetStream/Core 消费端点注册器</li>
 *   <li>{@link NatsMQBrokerAdapter} —— Broker 适配 SPI（绑定到 {@link MQBrokerAdapter}）</li>
 *   <li>{@link MQEventPublisher} —— 事件发布器（绑定到 NATS 实现）</li>
 * </ul>
 *
 * <p>与 Spring 版本的区别：{@link Connection}（jnats 连接）由业务方显式提供并传入 Module，
 * 而非由 Spring 自动创建（javalin 环境下显式管理外部资源更清晰）。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 1. 业务方创建 NATS 连接（需真实 NATS 服务）
 * Connection conn = Nats.connect("nats://127.0.0.1:4222");
 * // 2. 创建 Guice Module
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),
 *     new Ddd4jNatsMqGuiceModule(conn)
 * );
 * MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jNatsMqGuiceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jNatsMqGuiceModule.class);

    private final Connection connection;
    private final Ddd4jMQProperties mqProperties;

    /**
     * @param connection NATS 连接（业务方创建，如 {@code Nats.connect(servers)}）
     */
    public Ddd4jNatsMqGuiceModule(Connection connection) {
        this(connection, new Ddd4jMQProperties());
    }

    /**
     * @param connection    NATS 连接
     * @param mqProperties  ddd4j MQ 通用配置
     */
    public Ddd4jNatsMqGuiceModule(Connection connection, Ddd4jMQProperties mqProperties) {
        this.connection = connection;
        this.mqProperties = mqProperties;
    }

    @Override
    protected void configure() {
        bind(Connection.class).toInstance(connection);
    }

    /**
     * 提供消费端点注册器（对标 Spring 的 natsMQConsumerEndpointRegistrar Bean）。
     *
     * <p>注册 JVM 停机钩子，在应用关闭时清理 NATS 订阅/Dispatcher。
     */
    @Provides
    @Singleton
    public NatsMQConsumerEndpointRegistrar natsMQConsumerEndpointRegistrar() {
        NatsMQConsumerEndpointRegistrar registrar =
                new NatsMQConsumerEndpointRegistrar(connection, mqProperties);
        // 注册停机钩子（对标 Spring @Bean(destroyMethod = "close")）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing NatsMQConsumerEndpointRegistrar via JVM hook");
            registrar.close();
        }, "ddd4j-javalin-nats-shutdown"));
        log.info("NatsMQConsumerEndpointRegistrar initialized");
        return registrar;
    }

    /**
     * 提供 Broker 适配 SPI（对标 Spring 的 natsMQBrokerAdapter Bean）。
     */
    @Provides
    @Singleton
    public MQBrokerAdapter natsMQBrokerAdapter(NatsMQConsumerEndpointRegistrar registrar) {
        return new NatsMQBrokerAdapter(connection, mqProperties, registrar);
    }

    /**
     * 提供事件发布器（对标 Spring 的 natsMQEventPublisher Bean）。
     *
     * <p>绑定到 ddd4j-core 的 {@link MQEventPublisher}，让领域层通过统一契约发布事件。
     */
    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher() {
        return new NatsMQEventPublisher(connection, mqProperties);
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
