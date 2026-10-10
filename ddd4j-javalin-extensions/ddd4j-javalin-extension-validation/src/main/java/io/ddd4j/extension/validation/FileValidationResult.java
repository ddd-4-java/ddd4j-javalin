package io.ddd4j.extension.validation;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * 文件校验结果。
 *
 * 组件 valid：是否通过
 * 组件 failure：失败原因，通过时为空
 * 组件 detectedType：内容检测结果，可以为空
 */
public final class FileValidationResult {

    private static final long serialVersionUID = 0L;

    private final boolean valid;

    private final FileValidationFailure failure;

    private final DetectedFileType detectedType;

    /**
     * 创建成功结果。
     *
     * @param detectedType 检测类型
     * @return 成功结果
     */
    public static FileValidationResult valid(DetectedFileType detectedType) {
        return new FileValidationResult(true, null, detectedType);
    }

    /**
     * 创建失败结果。
     *
     * @param failure      失败原因
     * @param detectedType 已检测类型
     * @return 失败结果
     */
    public static FileValidationResult invalid(FileValidationFailure failure, DetectedFileType detectedType) {
        return new FileValidationResult(false, failure, detectedType);
    }

    @JsonCreator()
    public FileValidationResult(@JsonProperty("valid") boolean valid, @JsonProperty("failure") FileValidationFailure failure, @JsonProperty("detectedType") DetectedFileType detectedType) {
        this.valid = valid;
        this.failure = failure;
        this.detectedType = detectedType;
    }

    @JsonProperty("valid")
    public boolean valid() {
        return valid;
    }

    @JsonProperty("failure")
    public FileValidationFailure failure() {
        return failure;
    }

    @JsonProperty("detectedType")
    public DetectedFileType detectedType() {
        return detectedType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        FileValidationResult other = (FileValidationResult) obj;
        return this.valid == other.valid && Objects.equals(this.failure, other.failure) && Objects.equals(this.detectedType, other.detectedType);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Boolean.hashCode(valid);
        result = 31 * result + Objects.hashCode(failure);
        result = 31 * result + Objects.hashCode(detectedType);
        return result;
    }

    @Override
    public String toString() {
        return "FileValidationResult[valid=" + valid + ", failure=" + failure + ", detectedType=" + detectedType + "]";
    }
}
