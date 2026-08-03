package io.ddd4j.javalin.validation;

import io.ddd4j.extension.validation.FileValidationPolicy;
import io.ddd4j.extension.validation.FileValidationResult;
import io.ddd4j.extension.validation.FileValidationService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.UploadedFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Javalin 路由使用的上传文件校验入口。
 */
public final class JavalinFileValidator {

    private final FileValidationService validationService;

    public JavalinFileValidator(FileValidationService validationService) {
        this.validationService = Objects.requireNonNull(validationService, "validationService must not be null");
    }

    /**
     * 校验单个 Javalin 上传文件，失败时转换为 HTTP 400。
     *
     * @param uploadedFile 上传文件
     * @param policy 校验策略
     * @return 原上传文件
     */
    public UploadedFile validate(UploadedFile uploadedFile, FileValidationPolicy policy) {
        FileValidationResult result = validationService.validate(
                Objects.isNull(uploadedFile) ? null : new JavalinUploadedFileAdapter(uploadedFile), policy);
        if (!result.valid()) {
            throw new BadRequestResponse("上传文件校验失败",
                    Map.of("code", result.failure().name()));
        }
        return uploadedFile;
    }

    /**
     * 校验一组 Javalin 上传文件。
     *
     * @param uploadedFiles 上传文件列表
     * @param policy 校验策略
     * @return 原上传文件列表
     */
    public List<UploadedFile> validateAll(List<UploadedFile> uploadedFiles, FileValidationPolicy policy) {
        if (Objects.isNull(uploadedFiles) || uploadedFiles.isEmpty()) {
            validate(null, policy);
            return uploadedFiles;
        }
        uploadedFiles.forEach(uploadedFile -> validate(uploadedFile, policy));
        return uploadedFiles;
    }
}
