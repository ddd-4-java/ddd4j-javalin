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

    public Ddd4jValidationJavalinModule() {
        this(Collections.emptyList());
    }

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
