package io.ddd4j.extension.validation;

import io.ddd4j.kit.lang.StrKit;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 框架无关的上传文件校验服务。
 */
public final class FileValidationService {

    private final FileTypeDetector fileTypeDetector;
    private final Collection<FileContentCheckProvider> contentCheckProviders;

    public FileValidationService() {
        this(new OfficeFileTypeDetector(), Collections.emptyList());
    }

    public FileValidationService(FileTypeDetector fileTypeDetector,
            Collection<FileContentCheckProvider> contentCheckProviders) {
        this.fileTypeDetector = Objects.requireNonNull(fileTypeDetector, "fileTypeDetector must not be null");
        this.contentCheckProviders = ListSupport.copyOf(contentCheckProviders);
    }

    /**
     * 按策略校验单个文件。
     *
     * @param file 文件，可以为空
     * @param policy 校验策略
     * @return 校验结果
     */
    public FileValidationResult validate(ValidatableFile file, FileValidationPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        if (Objects.isNull(file) || file.size() <= 0) {
            return policy.isRequired()
                    ? FileValidationResult.invalid(FileValidationFailure.EMPTY, null)
                    : FileValidationResult.valid(null);
        }
        if (policy.getMaxSizeBytes() > 0 && file.size() > policy.getMaxSizeBytes()) {
            return FileValidationResult.invalid(FileValidationFailure.SIZE_EXCEEDED, null);
        }

        String declaredExtension = extensionOf(file.fileName());
        if (!policy.getAllowedExtensions().isEmpty()
                && !policy.getAllowedExtensions().contains(declaredExtension)) {
            return FileValidationResult.invalid(FileValidationFailure.EXTENSION_NOT_ALLOWED, null);
        }

        try {
            DetectedFileType detectedType = null;
            if (policy.isStrict()) {
                Optional<DetectedFileType> detected = fileTypeDetector.detect(file);
                if (detected.isEmpty()) {
                    return FileValidationResult.invalid(FileValidationFailure.TYPE_UNDETECTABLE, null);
                }
                detectedType = detected.get();
                if (!declaredExtension.equals(normalize(detectedType.extension()))) {
                    return FileValidationResult.invalid(FileValidationFailure.SIGNATURE_MISMATCH, detectedType);
                }
            }

            String mimeType = policy.isStrict() && Objects.nonNull(detectedType)
                    ? normalize(detectedType.mimeType())
                    : normalize(file.contentType());
            if (!policy.getAllowedMimeTypes().isEmpty()
                    && !policy.getAllowedMimeTypes().contains(mimeType)) {
                return FileValidationResult.invalid(FileValidationFailure.MIME_TYPE_NOT_ALLOWED, detectedType);
            }

            if (Objects.nonNull(detectedType) && !checkContent(file, detectedType)) {
                return FileValidationResult.invalid(FileValidationFailure.CONTENT_REJECTED, detectedType);
            }
            return FileValidationResult.valid(detectedType);
        } catch (IOException | RuntimeException exception) {
            return FileValidationResult.invalid(FileValidationFailure.READ_ERROR, null);
        }
    }

    /**
     * 校验失败时抛出统一异常。
     *
     * @param file 文件
     * @param policy 校验策略
     * @return 原文件
     */
    public ValidatableFile validateOrThrow(ValidatableFile file, FileValidationPolicy policy) {
        FileValidationResult result = validate(file, policy);
        if (!result.valid()) {
            throw new FileValidationException(result.failure());
        }
        return file;
    }

    private boolean checkContent(ValidatableFile file, DetectedFileType detectedType) throws IOException {
        for (FileContentCheckProvider provider : contentCheckProviders) {
            if (provider.supports(detectedType.extension()) && !provider.check(file, detectedType)) {
                return false;
            }
        }
        return true;
    }

    private String extensionOf(String fileName) {
        if (Objects.isNull(fileName) || StrKit.isBlank(fileName)) {
            return "";
        }
        int separatorIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex <= separatorIndex || extensionIndex == fileName.length() - 1) {
            return "";
        }
        return normalize(fileName.substring(extensionIndex + 1));
    }

    private String normalize(String value) {
        return Objects.isNull(value) ? "" : StrKit.trim(value).toLowerCase(Locale.ROOT);
    }

    private static final class ListSupport {

        private ListSupport() {
        }

        private static <T> Collection<T> copyOf(Collection<T> source) {
            return Objects.isNull(source) ? Collections.emptyList() : java.util.List.copyOf(source);
        }
    }
}
