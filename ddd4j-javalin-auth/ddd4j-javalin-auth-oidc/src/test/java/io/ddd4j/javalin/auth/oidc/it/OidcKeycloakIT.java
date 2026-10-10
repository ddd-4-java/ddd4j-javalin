package io.ddd4j.javalin.auth.oidc.it;

import com.nimbusds.jose.util.JSONObjectUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.ddd4j.javalin.auth.oidc.NimbusOidcTokenVerifier;
import io.ddd4j.javalin.auth.oidc.OidcHttpAuthentication;
import io.ddd4j.javalin.auth.oidc.OidcProperties;
import io.ddd4j.javalin.auth.oidc.OidcSubjectProvider;
import io.ddd4j.javalin.testcontainers.auth.KeycloakTestContainerFixture;
import io.javalin.Javalin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * OIDC 提供端集成测试：真实 Keycloak 容器（testcontainers 官方 keycloak 模块）+
 * 密码模式（Resource Owner Password Credentials）换取真实 access token，
 * 校验 {@link OidcHttpAuthentication} 的 Bearer 认证链路 —— 无 token 返回 401，
 * 携带 Keycloak 签发的 token（Nimbus JOSE 按 JWKS 验签）返回 200。
 *
 * <p>类级 {@code @Testcontainers(disabledWithoutDocker = true)}：无 Docker 环境时
 * 整个测试类自动跳过，不伪报通过。
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class OidcKeycloakIT {

    // 测试镜像来源: https://testcontainers.com/modules/keycloak/
    /** 真实 Keycloak 提供端（realm/client/user 由 fixture 的 realm 导入文件预置），随测试类启停。 */
    @Container
    private static final KeycloakContainer KEYCLOAK = new KeycloakTestContainerFixture().newContainer();

    @Test
    void shouldAllowRealKeycloakTokenAndRejectMissingToken() throws Exception {
        // 容器已由 TestcontainersExtension 在 beforeAll 启动，直接拼接 realm issuer 地址
        String issuer = KEYCLOAK.getAuthServerUrl() + "/realms/ddd4j-test";
        // 密码模式向 Keycloak 换取真实签名的 access token
        String token = requestToken(issuer);
        OidcSubjectProvider provider = new OidcSubjectProvider(new NimbusOidcTokenVerifier(
                OidcProperties.builder()
                        .issuer(issuer)
                        .jwksUri(URI.create(issuer + "/protocol/openid-connect/certs"))
                        .audiences(Collections.singleton("ddd4j-test-client"))
                        .build()));
        Javalin app = Javalin.create();
        try {
            OidcHttpAuthentication.register(app, provider, Collections.singleton("/health"));
            app.get("/protected", context -> context.result(provider.getSubject().getLoginIdAsString()));
            app.start(0);

            // 缺失 token → 401 拒绝
            assertEquals(401, get(app, null).statusCode());
            // 携带 Keycloak 真实 token → 200 放行
            assertEquals(200, get(app, "Bearer " + token).statusCode());
        } finally {
            app.stop();
        }
    }

    /** 密码模式向 Keycloak token 端点换取 access token，并断言 HTTP 200。 */
    private String requestToken(String issuer) throws Exception {
        String form = "grant_type=password&client_id=ddd4j-test-client&username=test-user&password=test-pass";
        HttpRequest request = HttpRequest.newBuilder(URI.create(issuer + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return (String) JSONObjectUtils.parse(response.body()).get("access_token");
    }

    /** 请求受保护端点并返回原始 HTTP 响应。 */
    private HttpResponse<String> get(Javalin app, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/protected"));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return HttpClient.newHttpClient().send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
