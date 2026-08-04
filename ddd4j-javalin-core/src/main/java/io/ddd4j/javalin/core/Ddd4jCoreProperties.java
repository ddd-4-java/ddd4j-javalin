package io.ddd4j.javalin.core;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for ddd4j-javalin-core, bound from {@code ddd4j.core.*} keys in
 * {@code application.yml} / {@code application.properties} / System properties / environment
 * variables (loaded by the consumer's bootstrap mechanism).
 *
 * <p>Aligned with {@code ddd4j-boot-core}'s core SPI lifecycle semantics: one master switch
 * ({@code enabled}) plus fine-grained sub-feature switches for the domain-event publisher
 * binding and the CQRS read-side projection SPI registration.
 */
@Getter
@Setter
public class Ddd4jCoreProperties {

    public static final String PREFIX = "ddd4j.core";

    /** Whether the ddd4j core layer (SPI bindings + GuiceContext registration) is enabled. Default {@code true}. */
    private boolean enabled = true;

    /**
     * Whether the {@code DomainEventPublisher} SPI binding is declared by
     * {@link Ddd4jCoreGuiceModule}. Default {@code true}.
     *
     * <p>Note: the upstream {@code Ddd4jGuiceModule} always provides a base
     * {@code DomainEventPublisher} binding; disabling this flag only skips the explicit
     * declaration in the core assembly (the base binding remains active).
     */
    private boolean domainEventsEnabled = true;

    /**
     * Whether the CQRS read-side projection SPI
     * ({@code SpiKeys.PROJECTION_POSITION_REPOSITORY}) is registered into the ddd4j global
     * context by {@link Ddd4jCoreAutoConfiguration}. Default {@code true}.
     */
    private boolean projectionEnabled = true;
}
