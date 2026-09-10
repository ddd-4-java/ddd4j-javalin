package io.ddd4j.javalin.web;

import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Production CORS allowlist contract. */
class Ddd4jJavalinCorsContractTest {

    @Test
    void shouldAllowOnlyConfiguredProductionOrigin() throws Exception {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setRuntimeMode(Ddd4jJavalinRuntimeMode.PRODUCTION);
        properties.setRequestLifecycle(false);
        properties.setIdempotencyEnabled(false);
        properties.setCors(true);
        properties.setAllowedOrigins(new String[]{"https://allowed.example"});
        Javalin app = Ddd4jJavalinApplication.run(properties, new String[0], "");
        app.get("/cors-production", context -> context.result("ok"));
        try {
            assertThat(request(app, "https://allowed.example").headers()
                    .firstValue("Access-Control-Allow-Origin")).contains("https://allowed.example");
            assertThat(request(app, "https://unknown.example").headers()
                    .firstValue("Access-Control-Allow-Origin")).isEmpty();
        } finally {
            app.stop();
        }
    }

    private HttpResponse<String> request(Javalin app, String origin) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(
                                "http://127.0.0.1:" + app.port() + "/cors-production"))
                        .header("Origin", origin).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
