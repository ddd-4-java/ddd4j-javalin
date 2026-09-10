package io.ddd4j.javalin.web;

import io.ddd4j.web.core.auth.AuthenticationMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Production configuration source and precedence contract. */
class Ddd4jJavalinPropertiesLoaderTest {

    @Test
    void shouldApplyCliSystemEnvironmentAndFilePrecedence() {
        Properties file = new Properties();
        file.setProperty("ddd4j.web.javalin.host", "file-host");
        file.setProperty("ddd4j.web.javalin.port", "1001");
        file.setProperty("ddd4j.web.javalin.context-path", "/file");
        file.setProperty("ddd4j.web.javalin.cors", "false");
        file.setProperty("ddd4j.web.javalin.max-upload-size-bytes", "1024");

        Map<String, String> environment = Map.of(
                "DDD4J_WEB_JAVALIN_HOST", "env-host",
                "DDD4J_WEB_JAVALIN_PORT", "1002",
                "DDD4J_WEB_JAVALIN_CONTEXT_PATH", "/env",
                "DDD4J_WEB_JAVALIN_CORS", "true",
                "DDD4J_WEB_JAVALIN_ALLOWED_ORIGINS", "https://one.example, https://two.example",
                "DDD4J_WEB_JAVALIN_REQUEST_LIFECYCLE", "false");

        Properties system = new Properties();
        system.setProperty("ddd4j.web.javalin.host", "system-host");
        system.setProperty("ddd4j.web.javalin.port", "1003");
        system.setProperty("ddd4j.web.javalin.request-timeout-ms", "2500");
        system.setProperty("ddd4j.web.javalin.public-paths", "/health,/public/**");
        system.setProperty("ddd4j.web.javalin.default-authentication-mode", "optional");
        system.setProperty("ddd4j.web.javalin.trust-forwarded-headers", "true");
        system.setProperty("ddd4j.web.javalin.idempotency-enabled", "true");
        system.setProperty("ddd4j.web.javalin.idempotency-cache-name", "shared-http");
        system.setProperty("ddd4j.web.javalin.idempotency-ttl-ms", "9000");
        system.setProperty("ddd4j.web.javalin.health-endpoint", "false");
        system.setProperty("ddd4j.web.javalin.runtime-mode", "production");

        Ddd4jJavalinProperties properties = Ddd4jJavalinPropertiesLoader.load(
                new String[]{"--port", "1004"}, file, environment, system);

        assertThat(properties.getHost()).isEqualTo("system-host");
        assertThat(properties.getPort()).isEqualTo(1004);
        assertThat(properties.getContextPath()).isEqualTo("/env");
        assertThat(properties.isCors()).isTrue();
        assertThat(properties.getAllowedOrigins()).containsExactly(
                "https://one.example", "https://two.example");
        assertThat(properties.getMaxUploadSizeBytes()).isEqualTo(1024L);
        assertThat(properties.getRequestTimeoutMs()).isEqualTo(2500L);
        assertThat(properties.isRequestLifecycle()).isFalse();
        assertThat(properties.getPublicPaths()).containsExactly("/health", "/public/**");
        assertThat(properties.getDefaultAuthenticationMode()).isEqualTo(AuthenticationMode.OPTIONAL);
        assertThat(properties.isTrustForwardedHeaders()).isTrue();
        assertThat(properties.isIdempotencyEnabled()).isTrue();
        assertThat(properties.getIdempotencyCacheName()).isEqualTo("shared-http");
        assertThat(properties.getIdempotencyTtl()).isEqualTo(Duration.ofSeconds(9));
        assertThat(properties.isHealthEndpoint()).isFalse();
        assertThat(properties.getRuntimeMode()).isEqualTo(Ddd4jJavalinRuntimeMode.PRODUCTION);
    }

    @Test
    void shouldRejectMalformedBooleanInsteadOfSilentlyDisablingSecurity() {
        Properties system = new Properties();
        system.setProperty("ddd4j.web.javalin.request-lifecycle", "yes");

        assertThatThrownBy(() -> Ddd4jJavalinPropertiesLoader.load(
                new String[0], new Properties(), Map.of(), system))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request-lifecycle");
    }

    @Test
    void shouldRejectMalformedPortInsteadOfUsingDefaultPort() {
        Properties system = new Properties();
        system.setProperty("ddd4j.web.javalin.port", "eight-thousand");

        assertThatThrownBy(() -> Ddd4jJavalinPropertiesLoader.load(
                new String[0], new Properties(), Map.of(), system))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }
}
