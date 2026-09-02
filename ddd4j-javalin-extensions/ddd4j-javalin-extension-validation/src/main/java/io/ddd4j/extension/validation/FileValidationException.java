package io.ddd4j.extension.validation;

import java.util.Objects;

/**
 * 文件未通过校验时由适配层使用的异常。
 */
public class FileValidationException extends RuntimeException {

    private final FileValidationFailure failure;

    public FileValidationException(FileValidationFailure failure) {
        super("Uploaded file validation failed: " + Objects.requireNonNull(failure, "failure must not be null"));
        this.failure = failure;
    }

    public FileValidationFailure getFailure() {
        return failure;
    }
}
