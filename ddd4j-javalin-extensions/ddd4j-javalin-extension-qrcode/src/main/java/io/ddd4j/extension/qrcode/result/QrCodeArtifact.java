package io.ddd4j.extension.qrcode.result;

import io.ddd4j.extension.qrcode.model.QrCodeOutput;
import lombok.Builder;
import lombok.Getter;

/** Generated QR code together with application correlation metadata. */
@Getter
@Builder
public final class QrCodeArtifact {

    private final String correlationId;
    private final String templateId;
    private final QrCodeOutput output;
}
