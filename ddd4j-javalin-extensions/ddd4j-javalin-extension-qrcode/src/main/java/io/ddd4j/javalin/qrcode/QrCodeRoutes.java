package io.ddd4j.javalin.qrcode;

import io.ddd4j.extension.qrcode.QrCodeService;
import io.ddd4j.extension.qrcode.command.DecodeQrCodeCommand;
import io.ddd4j.extension.qrcode.command.GenerateQrCodeCommand;
import io.ddd4j.extension.qrcode.result.QrCodeArtifact;
import io.github.hiwepy.zxing.exception.QrCodeErrorCode;
import io.github.hiwepy.zxing.exception.QrCodeException;
import io.github.hiwepy.zxing.model.QrCodeDecodeRequest;
import io.github.hiwepy.zxing.model.QrCodeImageFormat;
import io.github.hiwepy.zxing.model.QrCodeRequest;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
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
        app.unsafe.routes.post(config.getBasePath() + "/render", this::render);
        app.unsafe.routes.post(config.getBasePath() + "/base64", this::base64);
        app.unsafe.routes.post(config.getBasePath() + "/decode", this::decode);
        app.unsafe.routes.exception(QrCodeException.class, (exception, context) -> error(context,
                status(exception.getErrorCode()), exception.getErrorCode().name(), exception.getMessage()));
        app.unsafe.routes.exception(IllegalArgumentException.class, (exception, context) -> error(context,
                400, QrCodeErrorCode.QRCODE_INVALID_ARGUMENT.name(), exception.getMessage()));
    }

    private void render(Context context) {
        QrCodeArtifact artifact = generate(context);
        context.contentType(artifact.getOutput().getFormat().getMimeType())
                .result(artifact.getOutput().getBytes());
    }

    private void base64(Context context) {
        QrCodeArtifact artifact = generate(context);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("format", artifact.getOutput().getFormat().name());
        response.put("base64", artifact.getOutput().base64());
        response.put("dataUri", artifact.getOutput().dataUri());
        context.json(response);
    }

    private QrCodeArtifact generate(Context context) {
        String formatName = valueOrDefault(context.queryParam("format"), "PNG");
        int width = intOrDefault(context.queryParam("width"), 256);
        int height = intOrDefault(context.queryParam("height"), 256);
        QrCodeRequest request = QrCodeRequest.builder(context.body())
                .size(width, height)
                .format(QrCodeImageFormat.valueOf(formatName.toUpperCase()))
                .build();
        return service.generate(GenerateQrCodeCommand.builder().request(request).build());
    }

    private void decode(Context context) throws Exception {
        UploadedFile uploadedFile = context.uploadedFile("file");
        if (Objects.isNull(uploadedFile)) {
            throw new IllegalArgumentException("multipart field 'file' is required");
        }
        if (uploadedFile.size() > config.getMaxUploadBytes()) {
            throw new QrCodeException(QrCodeErrorCode.QRCODE_IMAGE_TOO_LARGE,
                    "QR code image exceeds configured upload limit");
        }
        try (InputStream inputStream = uploadedFile.content()) {
            context.json(service.decode(DecodeQrCodeCommand.builder()
                    .request(QrCodeDecodeRequest.from(inputStream)
                            .multiple(true)
                            .maxInputBytes(config.getMaxUploadBytes())
                            .build())
                    .build()));
        }
    }

    private void error(Context context, int status, String code, String message) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("code", code);
        response.put("message", message);
        context.status(status).json(response);
    }

    private int status(QrCodeErrorCode errorCode) {
        if (errorCode == QrCodeErrorCode.QRCODE_IMAGE_TOO_LARGE) {
            return 413;
        }
        if (errorCode == QrCodeErrorCode.QRCODE_UNSUPPORTED_FORMAT) {
            return 415;
        }
        if (errorCode == QrCodeErrorCode.QRCODE_DECODE_NOT_FOUND
                || errorCode == QrCodeErrorCode.QRCODE_CAPACITY_EXCEEDED
                || errorCode == QrCodeErrorCode.QRCODE_SELF_CHECK_FAILED) {
            return 422;
        }
        return errorCode == QrCodeErrorCode.QRCODE_INVALID_ARGUMENT ? 400 : 500;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    private int intOrDefault(String value, int defaultValue) {
        return Objects.isNull(value) ? defaultValue : Integer.parseInt(value);
    }
}
