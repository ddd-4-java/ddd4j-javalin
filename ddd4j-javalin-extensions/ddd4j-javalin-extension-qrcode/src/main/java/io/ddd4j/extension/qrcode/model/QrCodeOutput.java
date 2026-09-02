package io.ddd4j.extension.qrcode.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/** 二维码 PNG 输出。 */
@Getter
public final class QrCodeOutput {

    private final byte[] bytes;

    public QrCodeOutput(byte[] bytes) {
        this.bytes = Arrays.copyOf(Objects.requireNonNull(bytes, "bytes must not be null"), bytes.length);
    }
}
