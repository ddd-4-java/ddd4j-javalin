package io.ddd4j.javalin.web;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.web.core.BearerSubjectAuthenticator;
import io.ddd4j.web.core.PathWebAccessPolicy;
import io.ddd4j.web.core.WebExceptionTranslator;
import io.ddd4j.web.core.WebRequestContextFactory;
import io.ddd4j.web.core.WebRequestLifecycle;
import io.ddd4j.web.javalin.Ddd4jJavalinWeb;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies Guice assembly for {@link Ddd4jJavalinAutoConfiguration}: all ddd4j web layer
 * collaborators must be resolvable, and the disabled path must short-circuit.
 */
class Ddd4jJavalinAutoConfigurationTest {

    @Test
    void shouldResolveAllWebCollaborators() {
        Ddd4jJavalinAutoConfiguration module = Ddd4jJavalinAutoConfiguration.forTesting();
        Injector injector = Guice.createInjector(module);

        assertThat(injector.getInstance(Ddd4jJavalinProperties.class)).isNotNull();
        assertThat(injector.getInstance(Ddd4jJavalinWeb.class)).isNotNull();
        assertThat(injector.getInstance(WebRequestContextFactory.class)).isNotNull();
        assertThat(injector.getInstance(WebExceptionTranslator.class)).isNotNull();
        assertThat(injector.getInstance(BearerSubjectAuthenticator.class)).isNotNull();
        assertThat(injector.getInstance(PathWebAccessPolicy.class)).isNotNull();
        assertThat(injector.getInstance(WebRequestLifecycle.class)).isNotNull();
    }

    @Test
    void shouldRespectCustomPublicPaths() {
        Ddd4jJavalinAutoConfiguration module = Ddd4jJavalinAutoConfiguration
                .withPublicPaths("/open", "/docs", "/metrics");
        Injector injector = Guice.createInjector(module);

        PathWebAccessPolicy policy = injector.getInstance(PathWebAccessPolicy.class);
        assertThat(policy).isNotNull();
        assertThat(module.getProperties().getPublicPaths())
                .containsExactly("/open", "/docs", "/metrics");
    }

    @Test
    void shouldNotBindWebCollaboratorsWhenDisabled() {
        Ddd4jJavalinProperties props = new Ddd4jJavalinProperties();
        props.setEnabled(false);
        Ddd4jJavalinAutoConfiguration module = new Ddd4jJavalinAutoConfiguration(props);
        Injector injector = Guice.createInjector(module);

        // Ddd4jJavalinWeb must not be bound when disabled.
        assertThatThrownBy(() -> injector.getInstance(Ddd4jJavalinWeb.class))
                .isInstanceOfAny(CreationException.class, com.google.inject.ConfigurationException.class);
    }

    @Test
    void shouldExposeDefaultPublicPathsHelper() {
        assertThat(Ddd4jJavalinAutoConfiguration.defaultPublicPaths())
                .containsExactly("/health", "/health/readiness", "/health/liveness");
    }
}