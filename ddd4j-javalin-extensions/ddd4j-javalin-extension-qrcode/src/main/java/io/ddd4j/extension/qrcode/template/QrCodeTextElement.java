package io.ddd4j.extension.qrcode.template;

import lombok.Getter;

/** 文本外框元素。 */
@Getter
public final class QrCodeTextElement extends QrCodeFrameElement {

    private final String text;
    private final String fontName;
    private final int fontSize;
    private final boolean bold;
    private final String color;

    private QrCodeTextElement(Builder builder) {
        super(builder.x, builder.y, builder.width, builder.height, builder.zIndex);
        this.text = builder.text;
        this.fontName = builder.fontName;
        this.fontSize = builder.fontSize;
        this.bold = builder.bold;
        this.color = builder.color;
    }

    public static Builder builder(String text) {
        return new Builder(text);
    }

    public static final class Builder {
        private final String text;
        private int x;
        private int y;
        private int width;
        private int height;
        private int zIndex;
        private String fontName = "SansSerif";
        private int fontSize = 16;
        private boolean bold;
        private String color = "#000000";

        private Builder(String text) {
            this.text = text;
        }

        public Builder bounds(int x, int y, int width, int height) { this.x = x; this.y = y; this.width = width; this.height = height; return this; }
        public Builder zIndex(int zIndex) { this.zIndex = zIndex; return this; }
        public Builder font(String fontName, int fontSize, boolean bold) { this.fontName = fontName; this.fontSize = fontSize; this.bold = bold; return this; }
        public Builder color(String color) { this.color = color; return this; }
        public QrCodeTextElement build() { return new QrCodeTextElement(this); }
    }
}
