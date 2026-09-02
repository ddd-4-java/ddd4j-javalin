package io.ddd4j.extension.qrcode.model;

import lombok.Value;

/** 单个二维码解码结果。 */
@Value
public class QrCodeDecodeResult {

    String text;
}
