package io.ddd4j.javalin.validation;

import com.google.inject.AbstractModule;
import io.ddd4j.extension.validation.FileContentCheckProvider;
import io.ddd4j.extension.validation.FileValidationService;
import io.ddd4j.extension.validation.OfficeFileTypeDetector;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Javalin 文件校验能力的 Guice 装配模块。
 */
public final class Ddd4jValidationJavalinModule extends AbstractModule {

    private final List<FileContentCheckProvider> contentCheckProviders;

    /**
     * 构造 Ddd4jValidationJavalinModule Guice 模块实例。
     */
    public Ddd4jValidationJavalinModule() {
        this(Collections.emptyList());
    }

    /**
     * 构造 Ddd4jValidationJavalinModule Guice 模块实例。
     * @param contentCheckProviders 文件内容检查提供者集合。
     */
    public Ddd4jValidationJavalinModule(Collection<FileContentCheckProvider> contentCheckProviders) {
        this.contentCheckProviders = Objects.isNull(contentCheckProviders)
                ? Collections.emptyList()
                : List.copyOf(contentCheckProviders);
    }

    /**
     * 装配 Guice 绑定。
     */
    @Override
    protected void configure() {
        FileValidationService validationService = new FileValidationService(
                new OfficeFileTypeDetector(), contentCheckProviders);
        bind(FileValidationService.class).toInstance(validationService);
        bind(JavalinFileValidator.class);
    }
}
