package io.ddd4j.javalin.data.jpa;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.core.Ddd4jCoreGuiceModule;
import io.ddd4j.javalin.core.lifecycle.Ddd4jJavalinRuntime;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link Ddd4jJpaJavalinModule} propagates the underlying persistence-unit
 * resolution failure (no JPA provider on the classpath in unit tests). This proves the
 * module wires the EMF correctly: when a real provider is present the EMF is built, when
 * absent the error is surfaced cleanly.
 */
class Ddd4jJpaJavalinModuleTest {

    @Test
    void shouldNotCloseCallerOwnedEntityManagerFactory() {
        AtomicBoolean open = new AtomicBoolean(true);
        EntityManagerFactory factory = (EntityManagerFactory) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{EntityManagerFactory.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "isOpen" -> open.get();
                    case "close" -> {
                        open.set(false);
                        yield null;
                    }
                    case "toString" -> "recording-emf";
                    default -> null;
                });

        Injector injector = Guice.createInjector(
                Ddd4jCoreGuiceModule.defaults(), new Ddd4jJpaJavalinModule(factory));
        Ddd4jJavalinRuntime runtime = injector.getInstance(Ddd4jJavalinRuntime.class);
        runtime.start();
        runtime.close();

        assertThat(open).isTrue();
    }

    @Test
    void shouldFailCleanlyWhenNoJpaProviderOnClasspath() {
        // No jakarta.persistence.spi.PersistenceProvider is registered in unit-test scope;
        // the module must surface that as a CreationException, not silently swallow it.
        assertThatThrownBy(() -> Guice.createInjector(new Ddd4jJpaJavalinModule("missing-pu")))
                .isInstanceOf(CreationException.class);
    }
}
