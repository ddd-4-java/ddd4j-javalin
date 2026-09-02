package io.ddd4j.extension.qrcode.template;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;

/** Thread-safe in-memory QR template registry. */
public class InMemoryQrCodeTemplateRegistry implements QrCodeTemplateRegistry {

    private final ConcurrentMap<String, QrCodeTemplateDefinition> templates =
            new ConcurrentHashMap<String, QrCodeTemplateDefinition>();

    @Override
    public void register(QrCodeTemplateDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        templates.put(definition.getId(), definition);
    }

    @Override
    public Optional<QrCodeTemplateDefinition> find(String id) {
        if (StringUtils.isBlank(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(templates.get(id));
    }

    @Override
    public boolean remove(String id) {
        return StringUtils.isNotBlank(id) && Objects.nonNull(templates.remove(id));
    }
}
