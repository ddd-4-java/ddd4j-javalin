package io.ddd4j.javalin.web;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Startup validation contract for production-facing server properties. */
class Ddd4jJavalinPropertiesValidatorTest {

    @Test
    void shouldAcceptDevelopmentDefaults() {
        assertThatCode(() -> Ddd4jJavalinPropertiesValidator.validate(new Ddd4jJavalinProperties()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidNetworkAndLifecycleValues() {
        assertInvalid(properties -> properties.setHost(" "), "host");
        assertInvalid(properties -> properties.setPort(-1), "port");
        assertInvalid(properties -> properties.setPort(65536), "port");
        assertInvalid(properties -> properties.setContextPath("api"), "contextPath");
        assertInvalid(properties -> properties.setContextPath("/api/"), "contextPath");
        assertInvalid(properties -> properties.setMaxUploadSizeBytes(0), "maxUploadSizeBytes");
        assertInvalid(properties -> properties.setRequestTimeoutMs(0), "requestTimeoutMs");
        assertInvalid(properties -> properties.setIdempotencyTtl(Duration.ZERO), "idempotencyTtl");
        assertInvalid(properties -> properties.setPublicPaths(null), "publicPaths");
    }

    @Test
    void shouldRequireExplicitCorsOriginsInProduction() {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setRuntimeMode(Ddd4jJavalinRuntimeMode.PRODUCTION);
        properties.setCors(true);

        assertThatThrownBy(() -> Ddd4jJavalinPropertiesValidator.validate(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowedOrigins");
    }

    private void assertInvalid(java.util.function.Consumer<Ddd4jJavalinProperties> mutation,
                               String messageFragment) {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        mutation.accept(properties);
        assertThatThrownBy(() -> Ddd4jJavalinPropertiesValidator.validate(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(messageFragment);
    }
}
