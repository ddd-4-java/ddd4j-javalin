package io.ddd4j.javalin.testcontainers.auth;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;

/**
 * Shared Keycloak container fixture (used by {@code ddd4j-javalin-auth-*} integration tests
 * to verify end-to-end login flows against a real OIDC provider).
 *
 * <p>Uses the {@code quay.io/keycloak/keycloak:26.2.5} image in start-dev mode. The realm
 * "ddd4j-test" is auto-imported on startup, with a preconfigured client and a test user
 * ({@code test-user / test-pass}).
 *
 * <p>{@link #resolveConnectionString(KeycloakContainer)} returns the realm URL
 * ({@code http://host:port/realms/ddd4j-test}) consumed by sa-token / Spring Security /
 * Shiro realm resolvers.
 */
public class KeycloakTestContainerFixture extends AbstractTestContainerFixture<KeycloakContainer> {

    /** 默认容器镜像（钉版；原浮动别名 26.2 与 26.2.5 digest 一致）。 */
    // 测试镜像来源: https://testcontainers.com/modules/keycloak/
    // 说明: Keycloak 为社区模块（dasniko/testcontainers-keycloak），由 testcontainers-keycloak.version 管理。
    public static final String DEFAULT_IMAGE = "quay.io/keycloak/keycloak:26.2.5";
    /** 默认登录 realm。 */
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
