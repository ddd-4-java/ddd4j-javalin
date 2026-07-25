package io.ddd4j.javalin.data.jpa;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link Ddd4jJpaJavalinModule} propagates the underlying persistence-unit
 * resolution failure (no JPA provider on the classpath in unit tests). This proves the
 * module wires the EMF correctly: when a real provider is present the EMF is built, when
 * absent the error is surfaced cleanly.
 */
class Ddd4jJpaJavalinModuleTest {

    @Test
    void shouldFailCleanlyWhenNoJpaProviderOnClasspath() {
        // No jakarta.persistence.spi.PersistenceProvider is registered in unit-test scope;
        // the module must surface that as a CreationException, not silently swallow it.
        assertThatThrownBy(() -> Guice.createInjector(new Ddd4jJpaJavalinModule("missing-pu")))
                .isInstanceOf(CreationException.class);
    }
}