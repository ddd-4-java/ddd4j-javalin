package io.ddd4j.extension.qrcode.template;

import lombok.Getter;

/** 二维码外框元素的公共坐标属性。 */
@Getter
public abstract class QrCodeFrameElement {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int zIndex;

    protected QrCodeFrameElement(int x, int y, int width, int height, int zIndex) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.zIndex = zIndex;
    }
}
