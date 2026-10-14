package io.ddd4j.javalin.mq.core;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;

/**
 * Shared base for Javalin MQ broker Guice modules.
 *
 * <p>Subclasses bind:
 * <ul>
 *   <li>their broker-specific {@code XxxMQClient extends MQClient} as the
 *       {@link MQClient} contract,</li>
 *   <li>their {@code XxxMQProperties extends MQProperties} as
 *       {@link MQProperties}.</li>
 * </ul>
 *
 * <p>The actual wiring is intentionally minimal because ddd4j-mq 2.0.x exposes a
 * single {@code MQClient} contract per broker — broker modules own all producer and
 * consumer logic internally. Javalin just needs the {@code MQClient} bean to be
 * resolvable from Guice.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public abstract class AbstractDdd4jMqGuiceModule extends AbstractModule {

    private final MQProperties mqProperties;
    private final Ddd4jJavalinMqProperties integrationProperties;

    /**
     * 使用默认 {@link MQProperties} 与 {@link Ddd4jJavalinMqProperties} 构造模块骨架。
     */
    protected AbstractDdd4jMqGuiceModule() {
        this(new MQProperties(), new Ddd4jJavalinMqProperties());
    }

    /**
     * 使用指定 broker 属性、默认 Javalin 集成策略构造模块骨架。
     *
     * @param mqProperties broker 级 MQ 属性
     */
    protected AbstractDdd4jMqGuiceModule(MQProperties mqProperties) {
        this(mqProperties, new Ddd4jJavalinMqProperties());
    }

    /**
     * 使用指定 broker 属性与 Javalin 集成策略构造模块骨架。
     *
     * @param mqProperties broker 级 MQ 属性，不可为 {@code null}
     * @param integrationProperties Javalin MQ 生命周期策略，不可为 {@code null}
     * @throws IllegalArgumentException 当 {@code mqProperties} 为 {@code null}
     * @throws NullPointerException 当 {@code integrationProperties} 为 {@code null}
     */
    protected AbstractDdd4jMqGuiceModule(MQProperties mqProperties,
                                         Ddd4jJavalinMqProperties integrationProperties) {
        if (mqProperties == null) {
            throw new IllegalArgumentException("mqProperties must not be null");
        }
        this.mqProperties = mqProperties;
        this.integrationProperties = java.util.Objects.requireNonNull(
                integrationProperties, "integrationProperties must not be null");
    }

    @Override
    protected void configure() {
        bind(MQProperties.class).toInstance(mqProperties);
        bind(Ddd4jJavalinMqProperties.class).toInstance(integrationProperties);
        bind(JavalinMqListenerScanner.class).in(Singleton.class);
        bind(JavalinMqLifecycleParticipant.class)
                .toProvider(JavalinMqLifecycleParticipantProvider.class)
                .in(Singleton.class);
        Multibinder.newSetBinder(binder(), JavalinLifecycleParticipant.class)
                .addBinding().to(JavalinMqLifecycleParticipant.class);
    }

    /**
     * 返回本模块装配的 broker 级 MQ 属性，供子类绑定 broker 特定组件。
     *
     * @return broker 级 MQ 属性实例
     */
    protected MQProperties mqProperties() {
        return mqProperties;
    }
}
