package io.ddd4j.javalin.qrcode;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeRoutesTest {

    private Javalin app;
    private Ddd4jQrCodeJavalinModule module;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        module = new Ddd4jQrCodeJavalinModule(QrCodeModuleConfig.builder().routesEnabled(true).build());
        app = Javalin.create();
        module.registerRoutes(app);
        app.start(0);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() throws Exception {
        app.stop();
        module.close();
    }

    @Test
    void shouldRenderPngAndBase64() throws Exception {
        byte[] png = post("/qrcodes/render", "javalin-roundtrip", "text/plain").body();
        assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);

        HttpResponse<byte[]> base64 = post("/qrcodes/base64", "javalin-base64", "text/plain");
        assertThat(base64.statusCode()).isEqualTo(200);
        assertThat(new String(base64.body(), StandardCharsets.UTF_8)).contains("data:image/png;base64,");
    }

    @Test
    void shouldDecodeMultipartImage() throws Exception {
        byte[] png = post("/qrcodes/render", "multipart-roundtrip", "text/plain").body();
        String boundary = "ddd4j-qrcode-boundary";
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"code.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] multipart = new byte[prefix.length + png.length + suffix.length];
        System.arraycopy(prefix, 0, multipart, 0, prefix.length);
        System.arraycopy(png, 0, multipart, prefix.length, png.length);
        System.arraycopy(suffix, 0, multipart, prefix.length + png.length, suffix.length);

        HttpResponse<byte[]> response = post("/qrcodes/decode", multipart,
                "multipart/form-data; boundary=" + boundary);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).contains("multipart-roundtrip");
    }

    private HttpResponse<byte[]> post(String path, String body, String contentType) throws Exception {
        return post(path, body.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private HttpResponse<byte[]> post(String path, byte[] body, String contentType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
}
