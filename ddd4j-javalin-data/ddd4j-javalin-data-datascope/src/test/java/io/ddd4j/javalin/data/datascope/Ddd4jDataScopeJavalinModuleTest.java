package io.ddd4j.javalin.data.datascope;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.data.datascope.RequiresDataPermissionsValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link Ddd4jDataScopeJavalinModule} binds the data-permissions validator.
 */
class Ddd4jDataScopeJavalinModuleTest {

    @Test
    void shouldBindDataPermissionsValidator() {
        Injector injector = Guice.createInjector(new Ddd4jDataScopeJavalinModule());
        RequiresDataPermissionsValidator validator = injector.getInstance(RequiresDataPermissionsValidator.class);

        assertThat(validator).isNotNull();
    }
}