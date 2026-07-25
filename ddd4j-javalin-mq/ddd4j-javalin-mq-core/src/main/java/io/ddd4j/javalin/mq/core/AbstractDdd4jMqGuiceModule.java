package io.ddd4j.javalin.mq.core;

import com.google.inject.AbstractModule;

/**
 * Shared base for Javalin MQ broker modules.
 *
 * <p>Subclasses bind the broker-specific {@code MQBrokerAdapter} / {@code MQEventPublisher}
 * via {@link #configure()}. The base class is intentionally minimal because the ddd4j
 * mq-core SPI does not yet expose a stable {@code Ddd4jMQProperties} / {@code JsonMQMessageSerialization}
 * type usable outside Spring; concrete broker modules therefore own their own property
 * POJOs (e.g. {@code KafkaMQProperties}, {@code RabbitMQProperties}, …) and serialize
 * directly via the broker SDK.
 *
 * <p>Subclass contract:
 * <ul>
 *   <li>Override {@link #configure()} to bind {@code MQBrokerAdapter}, {@code MQEventPublisher},
 *       and any broker-specific configuration beans.</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public abstract class AbstractDdd4jMqGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        // No-op: subclasses bind broker-specific beans.
    }
}