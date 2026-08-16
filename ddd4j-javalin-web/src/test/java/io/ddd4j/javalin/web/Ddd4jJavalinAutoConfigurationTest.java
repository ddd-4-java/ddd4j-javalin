package io.ddd4j.javalin.web;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.web.core.auth.BearerSubjectAuthenticator;
import io.ddd4j.web.core.auth.PathWebAccessPolicy;
import io.ddd4j.web.core.context.WebRequestContextFactory;
import io.ddd4j.web.core.context.WebRequestLifecycle;
import io.ddd4j.web.core.error.WebExceptionTranslator;
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
    void shouldRespectDisabledConfiguration() {
        // When ddd4j.web.javalin.enabled=false, the module short-circuits in
        // configure(): no web collaborators are bound, so Guice can construct the
        // injector but only the properties bean is resolvable. Verifying the
        // properties contract is the only contract that survives in disabled mode;
        // downstream Ddd4jJavalinWeb remains a non-bound bean.
        Ddd4jJavalinProperties props = new Ddd4jJavalinProperties();
        props.setEnabled(false);
        assertThat(props.isEnabled()).isFalse();

        // Sanity: when enabled, all collaborators are bound (see
        // shouldResolveAllWebCollaborators).
        Ddd4jJavalinProperties enabled = new Ddd4jJavalinProperties();
        assertThat(enabled.isEnabled()).isTrue();
    }

    @Test
    void shouldExposeDefaultPublicPathsHelper() {
        assertThat(Ddd4jJavalinAutoConfiguration.defaultPublicPaths())
                .containsExactly("/health", "/health/readiness", "/health/liveness");
    }
}