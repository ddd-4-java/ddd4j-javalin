package io.ddd4j.extension.qrcode.template;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * Binds simple {@code ${name}} placeholders in reusable frame text elements.
 * Image data remains behind {@code QrCodeResourceResolver}; this binder never performs I/O.
 */
public class QrCodeTemplateBinder {

    /** Creates a new immutable frame and leaves the registered template unchanged. */
    public QrCodeFrame bind(QrCodeTemplateDefinition definition, Map<String, ?> variables) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(variables, "variables must not be null");
        QrCodeFrame source = definition.getFrame();
        QrCodeFrame.Builder target = QrCodeFrame.builder(source.getWidth(), source.getHeight())
                .backgroundColor(source.getBackgroundColor());
        for (QrCodeFrameElement element : source.getElements()) {
            target.addElement(copy(element, variables));
        }
        return target.build();
    }

    private QrCodeFrameElement copy(QrCodeFrameElement element, Map<String, ?> variables) {
        if (element instanceof QrCodeTextElement) {
            QrCodeTextElement text = (QrCodeTextElement) element;
            return QrCodeTextElement.builder(bindText(text.getText(), variables))
                    .bounds(text.getX(), text.getY(), text.getWidth(), text.getHeight())
                    .zIndex(text.getZIndex())
                    .font(text.getFontName(), text.getFontSize(), text.isBold())
                    .color(text.getColor())
                    .build();
        }
        if (element instanceof QrCodeImageElement) {
            QrCodeImageElement image = (QrCodeImageElement) element;
            return QrCodeImageElement.builder(image.getImage())
                    .bounds(image.getX(), image.getY(), image.getWidth(), image.getHeight())
                    .zIndex(image.getZIndex())
                    .build();
        }
        if (element instanceof QrCodeBlockElement) {
            QrCodeBlockElement block = (QrCodeBlockElement) element;
            return QrCodeBlockElement.builder()
                    .x(block.getX()).y(block.getY()).width(block.getWidth()).height(block.getHeight())
                    .zIndex(block.getZIndex())
                    .build();
        }
        return element;
    }

    private String bindText(String template, Map<String, ?> variables) {
        String bound = template;
        for (Map.Entry<String, ?> variable : variables.entrySet()) {
            String placeholder = "${" + variable.getKey() + "}";
            bound = bound.replace(placeholder,
                    Objects.isNull(variable.getValue()) ? StringUtils.EMPTY : String.valueOf(variable.getValue()));
        }
        return bound;
    }
}
