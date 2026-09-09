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
    private final IpRegionTemplate ipRegionTemplate;

    public Ddd4jExternalJavalinModule() {
        this(new ExternalProperties(), new SequenceProperties(), IpRegionTemplate.none());
    }

    public Ddd4jExternalJavalinModule(ExternalProperties externalProperties,
                                     SequenceProperties sequenceProperties) {
        this(externalProperties, sequenceProperties, IpRegionTemplate.none());
    }

    public Ddd4jExternalJavalinModule(ExternalProperties externalProperties,
                                     SequenceProperties sequenceProperties,
                                     IpRegionTemplate ipRegionTemplate) {
        this.externalProperties = Objects.requireNonNull(
                externalProperties, "externalProperties must not be null");
        this.sequenceProperties = Objects.requireNonNull(
                sequenceProperties, "sequenceProperties must not be null");
        this.ipRegionTemplate = Objects.requireNonNull(ipRegionTemplate, "ipRegionTemplate must not be null");
    }

    @Override
    protected void configure() {
        bind(IpRegionTemplate.class).toInstance(ipRegionTemplate);
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
        if (Objects.isNull(properties.getWorkerId()) || Objects.isNull(properties.getDataCenterId())) {
            throw new IllegalStateException("workerId and dataCenterId are required for GlobalSequence");
        }
        return new GlobalSequence(properties.getWorkerId(), properties.getDataCenterId(), properties.isUseSystemClock(),
                longValue(properties.getTimeOffset()), longValue(properties.getRandomSequenceLimit()));
    }

    private long longValue(Long value) {
        return Objects.isNull(value) ? 0L : value;
    }
}
