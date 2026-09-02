package io.ddd4j.extension.qrcode.template;

import java.util.Optional;

/** Registry for reusable QR outer-frame templates. */
public interface QrCodeTemplateRegistry {

    void register(QrCodeTemplateDefinition definition);

    Optional<QrCodeTemplateDefinition> find(String id);

    boolean remove(String id);
}
