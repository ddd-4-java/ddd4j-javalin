package io.ddd4j.javalin.data.logs;

import cn.hutool.core.lang.Snowflake;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.data.logs.ApiOperationLogProvider;
import io.ddd4j.data.logs.DefaultApiOperationLogProvider;
import io.ddd4j.data.logs.aspect.ApiOperationLogAspect;

/**
 * Guice Module wiring the ddd4j API operation log SPI for Javalin applications.
 *
 * <p>Aligned with ddd4j-boot's {@code Ddd4jApiLogAspectAutoConfiguration}: provides the
 * default {@link ApiOperationLogProvider} (which can be overridden by a user-defined
 * binding) and binds a singleton {@link ApiOperationLogAspect} ready to be applied to
 * {@code @ApiOperationLog}-annotated methods.
 *
 * <p>Note: aspect weaving happens via Guice's {@code bindInterceptor} when used. Consumers
 * that want automatic weaving should call {@code injector.getInstance(ApiOperationLogAspect.class)}
 * and register the interceptor manually with their bindings.
 */
public class Ddd4jApiLogJavalinModule extends AbstractModule {

    private final ApiOperationLogProvider provider;

    public Ddd4jApiLogJavalinModule() {
        this(new DefaultApiOperationLogProvider());
    }

    public Ddd4jApiLogJavalinModule(ApiOperationLogProvider provider) {
        this.provider = java.util.Objects.requireNonNull(provider, "provider must not be null");
    }

    @Override
    protected void configure() {
        bind(ApiOperationLogProvider.class).toInstance(provider);
    }

    @Provides
    @Singleton
    Snowflake snowflake() {
        return new Snowflake();
    }

    @Provides
    @Singleton
    ApiOperationLogAspect apiOperationLogAspect(Snowflake snowflake, ApiOperationLogProvider logProvider) {
        return new ApiOperationLogAspect(snowflake, logProvider);
    }
}
