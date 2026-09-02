package io.ddd4j.extension.validation;

/**
 * 文件校验失败类型，可由各框架转换为统一业务错误码。
 */
public enum FileValidationFailure {
    EMPTY,
    SIZE_EXCEEDED,
    EXTENSION_NOT_ALLOWED,
    MIME_TYPE_NOT_ALLOWED,
    TYPE_UNDETECTABLE,
    SIGNATURE_MISMATCH,
    CONTENT_REJECTED,
    READ_ERROR
}
