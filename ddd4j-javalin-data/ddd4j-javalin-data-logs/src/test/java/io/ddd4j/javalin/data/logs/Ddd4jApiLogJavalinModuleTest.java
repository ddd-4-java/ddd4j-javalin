package io.ddd4j.javalin.data.logs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.ddd4j.data.logs.ApiOperationLogProvider;
import io.ddd4j.data.logs.DefaultApiOperationLogProvider;
import io.ddd4j.data.logs.aspect.ApiOperationLogAspect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Guice assembly of {@link Ddd4jApiLogJavalinModule}: the
 * {@code ApiOperationLogProvider} is bound to its default implementation and the
 * aspect is resolvable.
 */
class Ddd4jApiLogJavalinModuleTest {

    @Test
    void shouldBindDefaultProviderAndAspect() {
        Injector injector = Guice.createInjector(new Ddd4jApiLogJavalinModule());

        ApiOperationLogProvider provider = injector.getInstance(ApiOperationLogProvider.class);
        ApiOperationLogAspect aspect = injector.getInstance(ApiOperationLogAspect.class);

        assertThat(provider).isNotNull().isInstanceOf(DefaultApiOperationLogProvider.class);
        assertThat(aspect).isNotNull();
    }

    @Test
    void shouldRespectUserOverriddenProvider() {
        ApiOperationLogProvider custom = new ApiOperationLogProvider() {
        };
        Injector injector = Guice.createInjector(
                new Ddd4jApiLogJavalinModule() {
                    @Override
                    protected void configure() {
                        bind(ApiOperationLogProvider.class).toInstance(custom);
                        bind(ApiOperationLogAspect.class).in(Singleton.class);
                    }
                });
        assertThat(injector.getInstance(ApiOperationLogProvider.class)).isSameAs(custom);
    }
}