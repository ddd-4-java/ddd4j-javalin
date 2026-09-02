package io.ddd4j.javalin.data.datascope;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.ddd4j.auth.datascope.DataScopeProvider;
import io.ddd4j.auth.datascope.RequiresDataPermissionsValidator;

/**
 * Guice Module wiring the ddd4j-data-datascope SPI for Javalin applications.
 *
 * <p>Aligned with ddd4j-boot's {@code Ddd4jDataScopeAutoConfiguration}: registers the
 * {@link RequiresDataPermissionsValidator} interceptor so any repository operation
 * annotated with {@code @RequiresDataPermissions} is filtered by the bound
 * {@link DataScopeProvider}.
 *
 * <p>Users should bind their own {@link DataScopeProvider} implementation (typically
 * tenant-aware); if no binding is provided the ddd4j-data-datascope default is used.
 */
public class Ddd4jDataScopeJavalinModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RequiresDataPermissionsValidator.class).in(Singleton.class);
    }
}