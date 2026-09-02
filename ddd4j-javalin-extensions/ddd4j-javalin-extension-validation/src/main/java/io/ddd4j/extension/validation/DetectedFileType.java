package io.ddd4j.extension.validation;

/**
 * 根据文件内容识别出的真实文件类型。
 *
 * @param extension 真实扩展名，不含点号
 * @param mimeType 真实 MIME 类型
 */
public record DetectedFileType(String extension, String mimeType) {
}
