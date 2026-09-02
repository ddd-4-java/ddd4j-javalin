package io.ddd4j.extension.qrcode.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.ddd4j.extension.qrcode.model.QrCodeDecodeResult;
import lombok.Getter;

/** Decoded QR code results with application correlation metadata. */
@Getter
public final class QrCodeScanResult {

    private final String correlationId;
    private final List<QrCodeDecodeResult> results;

    public QrCodeScanResult(String correlationId, List<QrCodeDecodeResult> results) {
        this.correlationId = correlationId;
        this.results = Collections.unmodifiableList(new ArrayList<QrCodeDecodeResult>(results));
    }
}
