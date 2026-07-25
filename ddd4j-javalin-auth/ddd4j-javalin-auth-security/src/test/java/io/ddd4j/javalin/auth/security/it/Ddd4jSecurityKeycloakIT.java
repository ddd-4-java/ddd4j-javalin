package io.ddd4j.javalin.auth.security.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.auth.KeycloakTestContainerFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton integration test for Spring Security ↔ Keycloak integration via the
 * {@code ddd4j-javalin-auth-security} module.
 *
 * <p>The full OAuth2 login flow (resource-server JWT validation against the Keycloak realm
 * JWKS) is implemented in the {@code ddd4j-javalin-sample-keycloak} module. This class
 * verifies the test wiring and serves as a regression guard for the
 * {@code testcontainers-keycloak} dependency.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jSecurityKeycloakIT {

    @SuppressWarnings("resource")
    private static final KeycloakContainer KEYCLOAK = new KeycloakTestContainerFixture().newContainer();

    @Test
    void shouldStartKeycloakContainer() {
        KEYCLOAK.start();
        try {
            assertThat(KEYCLOAK.getAuthServerUrl()).startsWith("http://");
        } finally {
            KEYCLOAK.stop();
        }
    }
}