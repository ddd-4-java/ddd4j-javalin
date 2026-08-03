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

    public JavalinUploadedFileAdapter(UploadedFile uploadedFile) {
        this.uploadedFile = Objects.requireNonNull(uploadedFile, "uploadedFile must not be null");
    }

    @Override
    public String fileName() {
        return uploadedFile.filename();
    }

    @Override
    public String contentType() {
        return uploadedFile.contentType();
    }

    @Override
    public long size() {
        return uploadedFile.size();
    }

    @Override
    public InputStream openStream() throws IOException {
        return uploadedFile.content();
    }
}
