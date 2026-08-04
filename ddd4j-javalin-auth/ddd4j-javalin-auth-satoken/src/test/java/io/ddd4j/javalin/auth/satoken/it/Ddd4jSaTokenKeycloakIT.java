package io.ddd4j.javalin.auth.satoken.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.auth.KeycloakTestContainerFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton integration test that verifies a {@link KeycloakContainer} can be started by the
 * {@link KeycloakTestContainerFixture}. The full OAuth2 / OIDC login round-trip
 * (acquiring a token from the realm, exchanging it for a sa-token session) is left to the
 * sample layer ({@code ddd4j-javalin-sample-keycloak}) where the consumer wiring lives.
 *
 * <p>This class exists to:
 * <ul>
 *   <li>Prove the {@code ddd4j-javalin-auth-satoken} module compiles against the
 *       Testcontainers Keycloak artifact (catches API drift early)</li>
 *   <li>Document the integration-test pattern for downstream consumers</li>
 * </ul>
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jSaTokenKeycloakIT {

    @SuppressWarnings("resource")
    private static final KeycloakContainer KEYCLOAK = new KeycloakTestContainerFixture().newContainer();

    @Test
    void shouldStartKeycloakContainer() {
        KEYCLOAK.start();
        try {
            assertThat(KEYCLOAK.getAuthServerUrl()).startsWith("http://");
            assertThat(KeycloakTestContainerFixture.DEFAULT_REALM).isNotBlank();
        } finally {
            KEYCLOAK.stop();
        }
    }
}