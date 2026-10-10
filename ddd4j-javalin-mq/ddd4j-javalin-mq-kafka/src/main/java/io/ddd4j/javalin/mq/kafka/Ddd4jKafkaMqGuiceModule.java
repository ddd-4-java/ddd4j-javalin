package io.ddd4j.javalin.mq.kafka;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.kafka.KafkaMQClient;
import io.ddd4j.mq.kafka.KafkaMQProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq KafkaMQClient (KafkaMQProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jKafkaMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final KafkaMQClient client;

    /**
     * 以默认 Kafka 属性构造模块。
     *
     * @param client 已初始化的 KafkaMQClient 实例，不可为 {@code null}
     */
    public Ddd4jKafkaMqGuiceModule(KafkaMQClient client) {
        this(client, new KafkaMQProperties());
    }

    /**
     * 以指定 broker 属性构造模块，并将客户端注册为单例。
     *
     * @param client 已初始化的 KafkaMQClient 实例，不可为 {@code null}
     * @param properties KafkaMQProperties 实例，不可为 {@code null}
     * @throws NullPointerException 当任一参数为 {@code null}
     */
    public Ddd4jKafkaMqGuiceModule(KafkaMQClient client, KafkaMQProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(KafkaMQProperties.class).toInstance((KafkaMQProperties) mqProperties());
        bind(KafkaMQClient.class).toInstance(client);
    }

    /**
     * 暴露单例 {@link MQClient} 契约供业务注入。
     *
     * @return 本模块持有的 KafkaMQClient 实例
     */
    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
