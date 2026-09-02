package io.ddd4j.extension.validation;

import java.io.IOException;
import java.io.InputStream;

/**
 * 框架无关的上传文件抽象，由各 Web 框架适配自身上传类型。
 */
public interface ValidatableFile {

    /**
     * 获取客户端文件名。
     *
     * @return 文件名
     */
    String fileName();

    /**
     * 获取客户端声明的 Content-Type。
     *
     * @return Content-Type，可以为空
     */
    String contentType();

    /**
     * 获取文件字节数。
     *
     * @return 文件大小
     */
    long size();

    /**
     * 打开一个新的内容流。
     *
     * @return 新输入流
     * @throws IOException 文件无法读取时抛出
     */
    InputStream openStream() throws IOException;
}
