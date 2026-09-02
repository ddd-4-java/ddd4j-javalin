package io.ddd4j.extension.qrcode.template;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 可复用的二维码外框模板。 */
@Getter
public final class QrCodeFrame {

    private final int width;
    private final int height;
    private final String backgroundColor;
    private final List<QrCodeFrameElement> elements;

    private QrCodeFrame(Builder builder) {
        this.width = builder.width;
        this.height = builder.height;
        this.backgroundColor = builder.backgroundColor;
        this.elements = Collections.unmodifiableList(new ArrayList<>(builder.elements));
    }

    public static Builder builder(int width, int height) {
        return new Builder(width, height);
    }

    public static final class Builder {
        private final int width;
        private final int height;
        private String backgroundColor = "#FFFFFF";
        private final List<QrCodeFrameElement> elements = new ArrayList<>();

        private Builder(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public Builder backgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder addElement(QrCodeFrameElement element) {
            this.elements.add(element);
            return this;
        }

        public QrCodeFrame build() {
            return new QrCodeFrame(this);
        }
    }
}
