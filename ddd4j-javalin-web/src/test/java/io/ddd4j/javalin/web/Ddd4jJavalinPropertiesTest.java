package io.ddd4j.javalin.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Ddd4jJavalinProperties}.
 */
class Ddd4jJavalinPropertiesTest {

    @Test
    void shouldDefaultToEnabledAnd8080() {
        Ddd4jJavalinProperties props = new Ddd4jJavalinProperties();
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getPort()).isEqualTo(8080);
        assertThat(props.getHost()).isEqualTo("0.0.0.0");
        assertThat(props.getContextPath()).isEqualTo("/");
        assertThat(props.isCors()).isFalse();
        assertThat(props.getMaxUploadSizeBytes()).isEqualTo(10L * 1024L * 1024L);
        assertThat(props.getRequestTimeoutMs()).isEqualTo(30_000L);
        assertThat(props.isRequestLifecycle()).isTrue();
        assertThat(props.isHealthEndpoint()).isTrue();
        assertThat(props.getPublicPaths()).contains("/health", "/health/readiness", "/health/liveness");
    }

    @Test
    void shouldRespectMutators() {
        Ddd4jJavalinProperties props = new Ddd4jJavalinProperties();
        props.setEnabled(false);
        props.setPort(9090);
        props.setContextPath("/api");
        props.setCors(true);
        props.setMaxUploadSizeBytes(50L * 1024L * 1024L);
        props.setRequestTimeoutMs(60_000L);
        props.setRequestLifecycle(false);
        props.setHealthEndpoint(false);
        props.setPublicPaths(new String[]{"/open", "/docs"});

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getPort()).isEqualTo(9090);
        assertThat(props.getContextPath()).isEqualTo("/api");
        assertThat(props.isCors()).isTrue();
        assertThat(props.getMaxUploadSizeBytes()).isEqualTo(50L * 1024L * 1024L);
        assertThat(props.getRequestTimeoutMs()).isEqualTo(60_000L);
        assertThat(props.isRequestLifecycle()).isFalse();
        assertThat(props.isHealthEndpoint()).isFalse();
        assertThat(props.getPublicPaths()).containsExactly("/open", "/docs");
    }

    @Test
    void shouldExposePrefixConstant() {
        assertThat(Ddd4jJavalinProperties.PREFIX).isEqualTo("ddd4j.web.javalin");
    }
}