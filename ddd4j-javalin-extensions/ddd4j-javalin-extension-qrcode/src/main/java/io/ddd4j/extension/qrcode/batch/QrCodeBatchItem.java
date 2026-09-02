package io.ddd4j.extension.qrcode.batch;

import io.ddd4j.extension.qrcode.command.GenerateQrCodeCommand;
import lombok.Builder;
import lombok.Getter;

/** One correlated item in a non-atomic QR generation batch. */
@Getter
@Builder
public final class QrCodeBatchItem {

    private final String itemId;
    private final GenerateQrCodeCommand command;
}
