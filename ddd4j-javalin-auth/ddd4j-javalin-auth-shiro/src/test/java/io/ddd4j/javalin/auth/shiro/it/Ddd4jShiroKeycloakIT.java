package io.ddd4j.javalin.auth.shiro.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.auth.KeycloakTestContainerFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton integration test for Apache Shiro ↔ Keycloak integration via the
 * {@code ddd4j-javalin-auth-shiro} module.
 *
 * <p>Shiro does not have first-class OIDC support, so the full login round-trip uses an
 * OIDC <em>subject</em> bridge (a custom Shiro {@code Realm} that resolves identities from
 * the Keycloak JWKS endpoint). That wiring lives in
 * {@code ddd4j-javalin-sample-keycloak}.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jShiroKeycloakIT {

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