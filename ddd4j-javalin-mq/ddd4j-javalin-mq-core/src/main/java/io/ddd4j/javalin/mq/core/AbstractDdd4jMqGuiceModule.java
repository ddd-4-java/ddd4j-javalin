package io.ddd4j.javalin.mq.core;

import com.google.inject.AbstractModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.serialization.JsonMQMessageSerialization;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.ddd4j.mq.serialization.MQMessageSerialization;

import java.util.Objects;

/**
 * Shared Guice bindings for Javalin MQ broker modules.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public abstract class AbstractDdd4jMqGuiceModule extends AbstractModule {

    private final Ddd4jMQProperties mqProperties;
    private final MQEventSerialization serialization;

    protected AbstractDdd4jMqGuiceModule() {
        this(new Ddd4jMQProperties(), new JsonMQMessageSerialization());
    }

    protected AbstractDdd4jMqGuiceModule(Ddd4jMQProperties mqProperties) {
        this(mqProperties, new JsonMQMessageSerialization());
    }

    protected AbstractDdd4jMqGuiceModule(Ddd4jMQProperties mqProperties, MQEventSerialization serialization) {
        this.mqProperties = Objects.requireNonNull(mqProperties, "mqProperties");
        this.serialization = Objects.requireNonNull(serialization, "serialization");
    }

    @Override
    protected void configure() {
        bind(Ddd4jMQProperties.class).toInstance(mqProperties);
        bind(MQEventSerialization.class).toInstance(serialization);
        if (serialization instanceof MQMessageSerialization messageSerialization) {
            bind(MQMessageSerialization.class).toInstance(messageSerialization);
        }
    }

    protected Ddd4jMQProperties mqProperties() {
        return mqProperties;
    }

    protected MQEventSerialization serialization() {
        return serialization;
    }
}
