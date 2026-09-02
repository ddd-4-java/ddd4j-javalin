package io.ddd4j.extension.qrcode.template;

import java.awt.image.BufferedImage;

/** 图片外框元素。 */
public final class QrCodeImageElement extends QrCodeFrameElement {

    private final BufferedImage image;

    private QrCodeImageElement(Builder builder) {
        super(builder.x, builder.y, builder.width, builder.height, builder.zIndex);
        this.image = builder.image;
    }

    public BufferedImage getImage() { return image; }
    public static Builder builder(BufferedImage image) { return new Builder(image); }

    public static final class Builder {
        private final BufferedImage image;
        private int x;
        private int y;
        private int width;
        private int height;
        private int zIndex;
        private Builder(BufferedImage image) { this.image = image; }
        public Builder bounds(int x, int y, int width, int height) { this.x = x; this.y = y; this.width = width; this.height = height; return this; }
        public Builder zIndex(int zIndex) { this.zIndex = zIndex; return this; }
        public QrCodeImageElement build() { return new QrCodeImageElement(this); }
    }
}
