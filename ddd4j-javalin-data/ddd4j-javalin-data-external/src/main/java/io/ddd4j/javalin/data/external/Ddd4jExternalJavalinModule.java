package io.ddd4j.javalin.data.external;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.data.external.ExternalProperties;
import io.ddd4j.data.external.SequenceProperties;
import io.ddd4j.data.external.region.IpRegionTemplate;
import io.ddd4j.data.external.sequence.GlobalSequence;

import java.util.Objects;

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

    private final ExternalProperties externalProperties;
    private final SequenceProperties sequenceProperties;

    public Ddd4jExternalJavalinModule() {
        this(new ExternalProperties(), new SequenceProperties());
    }

    public Ddd4jExternalJavalinModule(ExternalProperties externalProperties,
                                     SequenceProperties sequenceProperties) {
        this.externalProperties = Objects.requireNonNull(
                externalProperties, "externalProperties must not be null");
        this.sequenceProperties = Objects.requireNonNull(
                sequenceProperties, "sequenceProperties must not be null");
    }

    @Override
    protected void configure() {
        bind(IpRegionTemplate.class).toInstance(IpRegionTemplate.none());
    }

    @Provides
    @Singleton
    ExternalProperties externalProperties() {
        return externalProperties;
    }

    @Provides
    @Singleton
    SequenceProperties sequenceProperties() {
        return sequenceProperties;
    }

    @Provides
    @Singleton
    GlobalSequence globalSequence(SequenceProperties properties) {
        return new GlobalSequence(longValue(properties.getWorkerId()),
                longValue(properties.getDataCenterId()), properties.isUseSystemClock(),
                longValue(properties.getTimeOffset()), longValue(properties.getRandomSequenceLimit()));
    }

    private long longValue(Long value) {
        return Objects.isNull(value) ? 0L : value;
    }
}
