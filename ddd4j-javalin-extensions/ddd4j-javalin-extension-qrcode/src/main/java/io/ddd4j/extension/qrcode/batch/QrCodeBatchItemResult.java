package io.ddd4j.extension.qrcode.batch;

import io.ddd4j.extension.qrcode.result.QrCodeArtifact;
import lombok.Builder;
import lombok.Getter;

/** Success or failure of one QR generation batch item. */
@Getter
@Builder
public final class QrCodeBatchItemResult {

    private final String itemId;
    private final boolean success;
    private final QrCodeArtifact artifact;
    private final String errorCode;
    private final String errorMessage;
}
