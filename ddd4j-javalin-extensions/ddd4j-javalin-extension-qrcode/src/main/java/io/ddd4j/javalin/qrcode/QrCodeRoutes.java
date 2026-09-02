package io.ddd4j.javalin.qrcode;

import io.ddd4j.extension.qrcode.QrCodeService;
import io.ddd4j.extension.qrcode.command.DecodeQrCodeCommand;
import io.ddd4j.extension.qrcode.command.GenerateQrCodeCommand;
import io.ddd4j.extension.qrcode.model.QrCodeDecodeRequest;
import io.ddd4j.extension.qrcode.model.QrCodeRequest;
import io.ddd4j.extension.qrcode.result.QrCodeArtifact;
import io.ddd4j.extension.qrcode.result.QrCodeScanResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Explicit, opt-in Javalin routes. No remote URL fetch endpoint is provided. */
public class QrCodeRoutes {

    private final QrCodeService service;
    private final QrCodeModuleConfig config;

    public QrCodeRoutes(QrCodeService service, QrCodeModuleConfig config) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public void register(Javalin app) {
        Objects.requireNonNull(app, "app must not be null");
        // Javalin 6.7.0 基线：无 7.x 的 app.unsafe.routes，改用公开路由 API（语义一致）。
        app.post(config.getBasePath() + "/render", this::render);
        app.post(config.getBasePath() + "/base64", this::base64);
        app.post(config.getBasePath() + "/decode", this::decode);
        app.exception(IllegalArgumentException.class, (exception, context) -> error(context,
                400, "QRCODE_INVALID_ARGUMENT", exception.getMessage()));
    }

    private void render(Context context) {
        QrCodeArtifact artifact = generate(context);
        context.contentType("image/png").result(artifact.getOutput().getBytes());
    }

    private void base64(Context context) {
        QrCodeArtifact artifact = generate(context);
        byte[] bytes = artifact.getOutput().getBytes();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("format", "PNG");
        response.put("base64", Base64.getEncoder().encodeToString(bytes));
        response.put("dataUri", "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes));
        context.json(response);
    }

    private QrCodeArtifact generate(Context context) {
        int width = intOrDefault(context.queryParam("width"), 256);
        int height = intOrDefault(context.queryParam("height"), 256);
        QrCodeRequest request = QrCodeRequest.builder()
                .content(context.body())
                .width(width)
                .height(height)
                .build();
        return service.generate(GenerateQrCodeCommand.builder().request(request).build());
    }

    private void decode(Context context) throws IOException {
        UploadedFile uploadedFile = context.uploadedFile("file");
        if (Objects.isNull(uploadedFile)) {
            throw new IllegalArgumentException("multipart field 'file' is required");
        }
        if (uploadedFile.size() > config.getMaxUploadBytes()) {
            throw new IllegalArgumentException("QR code image exceeds configured upload limit");
        }
        byte[] bytes;
        try (InputStream inputStream = uploadedFile.content()) {
            bytes = inputStream.readAllBytes();
        }
        QrCodeScanResult scanResult = service.decode(DecodeQrCodeCommand.builder()
                .request(QrCodeDecodeRequest.from(bytes))
                .build());
        context.json(scanResult.getResults());
    }

    private void error(Context context, int status, String code, String message) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("code", code);
        response.put("message", message);
        context.status(status).json(response);
    }

    private int intOrDefault(String value, int defaultValue) {
        return Objects.isNull(value) ? defaultValue : Integer.parseInt(value);
    }
}
