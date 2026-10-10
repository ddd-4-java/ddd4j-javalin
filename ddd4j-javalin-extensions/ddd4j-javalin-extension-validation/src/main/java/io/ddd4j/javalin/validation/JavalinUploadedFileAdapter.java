package io.ddd4j.javalin.validation;

import io.ddd4j.extension.validation.ValidatableFile;
import io.javalin.http.UploadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * 将 Javalin {@link UploadedFile} 适配为框架无关文件对象。
 */
public final class JavalinUploadedFileAdapter implements ValidatableFile {

    private final UploadedFile uploadedFile;

    /**
     * 构造 JavalinUploadedFileAdapter 实例并初始化依赖。
     * @param uploadedFile Javalin 上传文件。
     */
    public JavalinUploadedFileAdapter(UploadedFile uploadedFile) {
        this.uploadedFile = Objects.requireNonNull(uploadedFile, "uploadedFile must not be null");
    }

    /**
     * 返回上传文件的原始文件名。
     * @return 原始文件名。
     */
    @Override
    public String fileName() {
        return uploadedFile.filename();
    }

    /**
     * 返回上传文件的 MIME 类型。
     * @return 文件 MIME 类型。
     */
    @Override
    public String contentType() {
        return uploadedFile.contentType();
    }

    /**
     * 返回文件字节数。
     * @return 文件字节数。
     */
    @Override
    public long size() {
        return uploadedFile.size();
    }

    /**
     * 打开文件内容流。
     * @return 方法执行结果。
     * @throws IOException 打开文件流失败时抛出。
     */
    @Override
    public InputStream openStream() throws IOException {
        return uploadedFile.content();
    }
}
