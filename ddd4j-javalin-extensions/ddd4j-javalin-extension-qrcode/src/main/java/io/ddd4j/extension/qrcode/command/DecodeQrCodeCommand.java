package io.ddd4j.extension.qrcode.command;

import io.ddd4j.extension.qrcode.model.QrCodeDecodeRequest;
import lombok.Builder;
import lombok.Getter;

/** Application command for QR code decoding. */
@Getter
@Builder
public final class DecodeQrCodeCommand {

    private final String correlationId;
    private final QrCodeDecodeRequest request;
}
