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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
class OidcKeycloakIT {

    @Test
    void shouldAllowRealKeycloakTokenAndRejectMissingToken() throws Exception {
        try (KeycloakContainer keycloak = new KeycloakTestContainerFixture().newContainer()) {
            keycloak.start();
            String issuer = keycloak.getAuthServerUrl() + "/realms/ddd4j-test";
            String token = requestToken(issuer);
            OidcSubjectProvider provider = new OidcSubjectProvider(new NimbusOidcTokenVerifier(
                    OidcProperties.builder()
                            .issuer(issuer)
                            .jwksUri(URI.create(issuer + "/protocol/openid-connect/certs"))
                            .audiences(Set.of("ddd4j-test-client"))
                            .build()));
            Javalin app = Javalin.create();
            try {
                OidcHttpAuthentication.register(app, provider, Set.of("/health"));
                app.unsafe.routes.get("/protected", context -> context.result(provider.getSubject().getLoginIdAsString()));
                app.start(0);

                assertEquals(401, get(app, null).statusCode());
                assertEquals(200, get(app, "Bearer " + token).statusCode());
            } finally {
                app.stop();
            }
        }
    }

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

    private HttpResponse<String> get(Javalin app, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/protected"));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return HttpClient.newHttpClient().send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
