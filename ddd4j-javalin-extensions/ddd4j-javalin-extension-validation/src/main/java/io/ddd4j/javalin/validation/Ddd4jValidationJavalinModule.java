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
     * 无参构造：不附加任何文件内容检查提供者。
     */
    public Ddd4jValidationJavalinModule() {
        this(Collections.emptyList());
    }

    /**
     * 构造函数：指定文件内容检查提供者集合。
     *
     * @param contentCheckProviders 内容检查提供者集合，为 {@code null} 时视为空集合
     */
    public Ddd4jValidationJavalinModule(Collection<FileContentCheckProvider> contentCheckProviders) {
        this.contentCheckProviders = Objects.isNull(contentCheckProviders)
                ? Collections.emptyList()
                : List.copyOf(contentCheckProviders);
    }

    @Override
    protected void configure() {
        FileValidationService validationService = new FileValidationService(
                new OfficeFileTypeDetector(), contentCheckProviders);
        bind(FileValidationService.class).toInstance(validationService);
        bind(JavalinFileValidator.class);
    }
}
