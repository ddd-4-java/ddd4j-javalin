package io.ddd4j.extension.qrcode.template;

/** 二维码区域占位元素。 */
public final class QrCodeBlockElement extends QrCodeFrameElement {

    private QrCodeBlockElement(Builder builder) {
        super(builder.x, builder.y, builder.width, builder.height, builder.zIndex);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int x;
        private int y;
        private int width;
        private int height;
        private int zIndex;
        public Builder x(int x) { this.x = x; return this; }
        public Builder y(int y) { this.y = y; return this; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder height(int height) { this.height = height; return this; }
        public Builder zIndex(int zIndex) { this.zIndex = zIndex; return this; }
        public QrCodeBlockElement build() { return new QrCodeBlockElement(this); }
    }
}
