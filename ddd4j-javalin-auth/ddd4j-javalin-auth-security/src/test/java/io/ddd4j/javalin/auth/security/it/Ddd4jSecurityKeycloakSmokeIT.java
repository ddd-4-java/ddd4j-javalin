package io.ddd4j.javalin.auth.security.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.auth.KeycloakTestContainerFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keycloak 容器夹具冒烟测试，不代表 Spring Security 已安装 OIDC/JWT bridge。
 *
 * <p>本测试只验证 testcontainers-keycloak 夹具可启动；真实 token/JWKS/HTTP allow-deny
 * 由 {@code ddd4j-javalin-auth-oidc} 的 Keycloak IT 独立证明。
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jSecurityKeycloakSmokeIT {

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
