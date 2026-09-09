package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.auth.AuthPrincipal;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OidcHttpAuthenticationTest {

    private Javalin app;

    @AfterEach
    void stopApp() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void shouldAllowPublicPathAndProtectSecurePath() throws Exception {
        OidcSubjectProvider provider = new OidcSubjectProvider(token -> {
            if ("outage".equals(token)) {
                throw new OidcProviderUnavailableException("internal endpoint", new IllegalStateException());
            }
            if (!"valid".equals(token)) {
                throw new OidcAuthenticationException("invalid detail");
            }
            return new AuthPrincipal().setLoginId("user-42");
        });
        app = Javalin.create();
        OidcHttpAuthentication.register(app, provider, Set.of("/public"));
        app.get("/public", context -> context.result("public"));
        app.get("/secure", context -> context.result(provider.getSubject().getLoginIdAsString()));
        app.start(0);

        assertEquals(200, get("/public", null).statusCode());
        assertEquals(401, get("/secure", null).statusCode());
        assertEquals(401, get("/secure", "Bearer invalid").statusCode());
        HttpResponse<String> unavailable = get("/secure", "Bearer outage");
        assertEquals(503, unavailable.statusCode());
        assertFalse(unavailable.body().contains("internal endpoint"));
        HttpResponse<String> allowed = get("/secure", "Bearer valid");
        assertEquals(200, allowed.statusCode());
        assertEquals("user-42", allowed.body());
    }

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return HttpClient.newHttpClient().send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
