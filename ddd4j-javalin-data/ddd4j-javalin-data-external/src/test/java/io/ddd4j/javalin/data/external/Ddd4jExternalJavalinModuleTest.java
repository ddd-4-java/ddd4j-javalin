package io.ddd4j.javalin.data.external;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.data.external.ExternalProperties;
import io.ddd4j.data.external.SequenceProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Guice assembly of {@link Ddd4jExternalJavalinModule}: the two property holders
 * must be resolvable as singletons.
 */
class Ddd4jExternalJavalinModuleTest {

    @Test
    void shouldBindBothPropertyHolders() {
        Injector injector = Guice.createInjector(new Ddd4jExternalJavalinModule());

        ExternalProperties external = injector.getInstance(ExternalProperties.class);
        SequenceProperties sequence = injector.getInstance(SequenceProperties.class);

        assertThat(external).isNotNull();
        assertThat(sequence).isNotNull();
    }

    @Test
    void shouldReturnSameInstanceForRepeatedLookups() {
        Injector injector = Guice.createInjector(new Ddd4jExternalJavalinModule());
        assertThat(injector.getInstance(ExternalProperties.class))
                .isSameAs(injector.getInstance(ExternalProperties.class));
    }
}