package io.ddd4j.extension.validation;

import java.io.IOException;
import java.io.InputStream;

/**
 * 可重复打开文件内容流的来源。
 */
@FunctionalInterface
public interface InputStreamSource {

    /**
     * 打开一个新的输入流，调用方负责关闭。
     *
     * @return 新输入流
     * @throws IOException 文件无法读取时抛出
     */
    InputStream openStream() throws IOException;
}
