package io.ddd4j.javalin.auth.satoken.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.auth.KeycloakTestContainerFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keycloak 容器夹具冒烟测试，不代表 Sa-Token 已安装 OIDC token/session bridge。
 *
 * <p>This class exists to:
 * <ul>
 *   <li>Prove the {@code ddd4j-javalin-auth-satoken} module compiles against the
 *       Testcontainers Keycloak artifact (catches API drift early)</li>
 *   <li>明确真实 OIDC token 验证仅由 {@code ddd4j-javalin-auth-oidc} 的 Keycloak IT 证明</li>
 * </ul>
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jSaTokenKeycloakSmokeIT {

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
