package io.ddd4j.extension.validation;

import io.ddd4j.kit.lang.StrKit;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 不可变文件校验策略。
 */
public final class FileValidationPolicy {

    private final boolean required;
    private final boolean strict;
    private final long maxSizeBytes;
    private final Set<String> allowedExtensions;
    private final Set<String> allowedMimeTypes;

    private FileValidationPolicy(Builder builder) {
        this.required = builder.required;
        this.strict = builder.strict;
        this.maxSizeBytes = builder.maxSizeBytes;
        this.allowedExtensions = immutableNormalized(builder.allowedExtensions);
        this.allowedMimeTypes = immutableNormalized(builder.allowedMimeTypes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isStrict() {
        return strict;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public Set<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    private static Set<String> immutableNormalized(Set<String> values) {
        LinkedHashSet<String> normalized = values.stream()
                .filter(Objects::nonNull)
                .filter(StrKit::isNotBlank)
                .map(value -> StrKit.trim(value).toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(normalized);
    }

    /**
     * 文件校验策略构建器。
     */
    public static final class Builder {

        private boolean required = true;
        private boolean strict = true;
        private long maxSizeBytes = 2L * 1024L * 1024L;
        private Set<String> allowedExtensions = new LinkedHashSet<>();
        private Set<String> allowedMimeTypes = new LinkedHashSet<>();

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        public Builder maxSizeBytes(long maxSizeBytes) {
            if (maxSizeBytes < 0) {
                throw new IllegalArgumentException("maxSizeBytes must be greater than or equal to zero");
            }
            this.maxSizeBytes = maxSizeBytes;
            return this;
        }

        public Builder allowedExtensions(String... allowedExtensions) {
            this.allowedExtensions = new LinkedHashSet<>(Arrays.asList(allowedExtensions));
            return this;
        }

        public Builder allowedMimeTypes(String... allowedMimeTypes) {
            this.allowedMimeTypes = new LinkedHashSet<>(Arrays.asList(allowedMimeTypes));
            return this;
        }

        public FileValidationPolicy build() {
            return new FileValidationPolicy(this);
        }
    }
}
