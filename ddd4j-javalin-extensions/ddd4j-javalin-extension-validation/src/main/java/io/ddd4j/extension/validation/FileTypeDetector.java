package io.ddd4j.extension.validation;

import java.io.IOException;
import java.util.Optional;

/**
 * 文件内容类型检测策略。
 */
@FunctionalInterface
public interface FileTypeDetector {

    /**
     * 根据内容识别文件类型。
     *
     * @param file 文件
     * @return 检测结果；无法识别时为空
     * @throws IOException 文件无法读取时抛出
     */
    Optional<DetectedFileType> detect(ValidatableFile file) throws IOException;
}
