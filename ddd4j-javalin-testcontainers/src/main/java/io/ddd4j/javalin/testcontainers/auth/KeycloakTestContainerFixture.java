package io.ddd4j.javalin.testcontainers.auth;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;

/**
 * Shared Keycloak container fixture (used by {@code ddd4j-javalin-auth-*} integration tests
 * to verify end-to-end login flows against a real OIDC provider).
 *
 * <p>Uses the {@code quay.io/keycloak/keycloak:26.2} image in start-dev mode. The realm
 * "ddd4j-test" is auto-imported on startup, with a preconfigured client and a test user
 * ({@code test-user / test-pass}).
 *
 * <p>{@link #resolveConnectionString(KeycloakContainer)} returns the realm URL
 * ({@code http://host:port/realms/ddd4j-test}) consumed by sa-token / Spring Security /
 * Shiro realm resolvers.
 */
public class KeycloakTestContainerFixture extends AbstractTestContainerFixture<KeycloakContainer> {

    public static final String DEFAULT_IMAGE = "quay.io/keycloak/keycloak:26.2";
    public static final String DEFAULT_REALM = "ddd4j-test";

    @Override
    public KeycloakContainer newContainer() {
        return new KeycloakContainer(DEFAULT_IMAGE).withRealmImportFile("keycloak/ddd4j-test-realm.json");
    }

    @Override
    protected String resolveConnectionString(KeycloakContainer container) {
        return container.getAuthServerUrl() + "/realms/" + DEFAULT_REALM;
    }
}
