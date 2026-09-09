package io.ddd4j.javalin.data.datascope;

import com.google.inject.AbstractModule;
import io.ddd4j.data.datascope.DataScopeProvider;
import io.ddd4j.data.datascope.RequiresDataPermissionsValidator;

import java.util.Objects;

/**
 * Guice Module wiring the ddd4j-data-datascope SPI for Javalin applications.
 *
 * <p>Aligned with ddd4j-boot's {@code Ddd4jDataScopeAutoConfiguration}: registers the
 * {@link RequiresDataPermissionsValidator} interceptor so any repository operation
 * annotated with {@code @RequiresDataPermissions} is filtered by the bound
 * {@link DataScopeProvider}.
 *
 * <p>Users should bind their own {@link DataScopeProvider} implementation (typically
 * tenant-aware). The no-argument module is fail-closed and denies every value.
 */
public class Ddd4jDataScopeJavalinModule extends AbstractModule {

    private final DataScopeProvider provider;

    public Ddd4jDataScopeJavalinModule() {
        this((dataType, data) -> false);
    }

    public Ddd4jDataScopeJavalinModule(DataScopeProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
    }

    @Override
    protected void configure() {
        bind(DataScopeProvider.class).toInstance(provider);
        bind(RequiresDataPermissionsValidator.class)
                .toInstance(new RequiresDataPermissionsValidator(provider));
    }
}
