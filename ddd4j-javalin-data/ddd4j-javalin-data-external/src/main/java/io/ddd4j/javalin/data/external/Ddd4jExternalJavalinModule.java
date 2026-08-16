package io.ddd4j.javalin.data.external;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.data.external.ExternalProperties;
import io.ddd4j.data.external.SequenceProperties;

/**
 * Guice Module for ddd4j-data-external wiring in Javalin applications.
 *
 * <p>Aligned with ddd4j-boot's {@code Ddd4jExternalAutoConfiguration}: binds the
 * {@link ExternalProperties} and {@link SequenceProperties} configuration holders so that
 * downstream modules (REST client, region cache, IP/weather templates) can be injected.
 *
 * <p>The actual bean assembly (RestClient / IpRegionTemplate / WeatherTemplate) is left to
 * ddd4j-data-external's own Guice integration; this module only ensures the property
 * holders exist in the Javalin Guice context.
 */
public class Ddd4jExternalJavalinModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bindings provided by @Provides methods below
    }

    @Provides
    @Singleton
    ExternalProperties externalProperties() {
        return new ExternalProperties();
    }

    @Provides
    @Singleton
    SequenceProperties sequenceProperties() {
        return new SequenceProperties();
    }
}