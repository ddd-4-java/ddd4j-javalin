package io.ddd4j.javalin.data.logs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.data.logs.aspect.ApiOperationLogProvider;
import io.ddd4j.data.logs.aspect.DefaultApiOperationLogProvider;
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
                    }
                });
        assertThat(injector.getInstance(ApiOperationLogProvider.class)).isSameAs(custom);
        assertThat(injector.getInstance(ApiOperationLogAspect.class)).isNotNull();
    }
}