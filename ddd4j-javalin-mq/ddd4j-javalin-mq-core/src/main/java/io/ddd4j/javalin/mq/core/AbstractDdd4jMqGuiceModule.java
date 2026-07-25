package io.ddd4j.javalin.mq.core;

import com.google.inject.AbstractModule;
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

    protected AbstractDdd4jMqGuiceModule() {
        this(new MQProperties());
    }

    protected AbstractDdd4jMqGuiceModule(MQProperties mqProperties) {
        if (mqProperties == null) {
            throw new IllegalArgumentException("mqProperties must not be null");
        }
        this.mqProperties = mqProperties;
    }

    @Override
    protected void configure() {
        bind(MQProperties.class).toInstance(mqProperties);
    }

    protected MQProperties mqProperties() {
        return mqProperties;
    }
}