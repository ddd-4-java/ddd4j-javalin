package io.ddd4j.extension.qrcode.model;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/** 二维码生成请求。 */
@Getter
@Builder
public final class QrCodeRequest {

    private final String content;
    @Builder.Default
    private final int width = 300;
    @Builder.Default
    private final int height = 300;

    public QrCodeRequest(String content, int width, int height) {
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("content must not be blank");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.content = content;
        this.width = width;
        this.height = height;
    }
}
