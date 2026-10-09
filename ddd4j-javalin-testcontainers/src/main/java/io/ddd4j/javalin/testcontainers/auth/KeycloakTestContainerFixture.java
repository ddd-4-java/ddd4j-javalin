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
    /**
     * 默认构造方法：Fixture 仅持有镜像与端口常量，容器由 {@link #newContainer()} 构建。
     */
    public KeycloakTestContainerFixture() {
    }

    /** 默认容器镜像。 */
    public static final String DEFAULT_IMAGE = "quay.io/keycloak/keycloak:26.2";
    /** 默认 realm 名。 */
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
