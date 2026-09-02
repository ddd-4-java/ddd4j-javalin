package io.ddd4j.extension.qrcode.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/** 二维码解码请求。 */
@Getter
public final class QrCodeDecodeRequest {

    private final byte[] bytes;

    @Builder
    public QrCodeDecodeRequest(byte[] bytes) {
        this.bytes = Arrays.copyOf(Objects.requireNonNull(bytes, "bytes must not be null"), bytes.length);
    }

    public static QrCodeDecodeRequest from(byte[] bytes) {
        return new QrCodeDecodeRequest(bytes);
    }
}
