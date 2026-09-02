package io.ddd4j.extension.validation;

/**
 * 文件校验结果。
 *
 * @param valid 是否通过
 * @param failure 失败原因，通过时为空
 * @param detectedType 内容检测结果，可以为空
 */
public record FileValidationResult(boolean valid, FileValidationFailure failure, DetectedFileType detectedType) {

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
     * @param failure 失败原因
     * @param detectedType 已检测类型
     * @return 失败结果
     */
    public static FileValidationResult invalid(FileValidationFailure failure, DetectedFileType detectedType) {
        return new FileValidationResult(false, failure, detectedType);
    }
}
