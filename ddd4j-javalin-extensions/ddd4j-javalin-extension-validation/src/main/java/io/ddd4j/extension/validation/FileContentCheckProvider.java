package io.ddd4j.extension.validation;

import java.io.IOException;

/**
 * 面向具体文件格式的扩展内容检查器。
 */
public interface FileContentCheckProvider {

    /**
     * 判断是否支持指定真实扩展名。
     *
     * @param extension 扩展名
     * @return 是否支持
     */
    boolean supports(String extension);

    /**
     * 校验文件内部业务内容。
     *
     * @param file 文件
     * @param detectedType 真实文件类型
     * @return 是否通过
     * @throws IOException 文件无法读取时抛出
     */
    boolean check(ValidatableFile file, DetectedFileType detectedType) throws IOException;
}
