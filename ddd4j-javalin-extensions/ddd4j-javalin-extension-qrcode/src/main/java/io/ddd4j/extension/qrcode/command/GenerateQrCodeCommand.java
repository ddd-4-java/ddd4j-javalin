package io.ddd4j.extension.qrcode.command;

import io.ddd4j.extension.qrcode.model.QrCodeRequest;
import lombok.Builder;
import lombok.Getter;

/** Application command for QR code generation. */
@Getter
@Builder
public final class GenerateQrCodeCommand {

    private final String correlationId;
    private final String templateId;
    private final QrCodeRequest request;
}
