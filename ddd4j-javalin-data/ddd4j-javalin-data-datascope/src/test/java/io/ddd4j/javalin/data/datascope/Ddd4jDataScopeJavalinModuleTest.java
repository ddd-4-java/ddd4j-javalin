package io.ddd4j.javalin.data.datascope;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.data.datascope.DataScopeProvider;
import io.ddd4j.data.datascope.RequiresDataPermissionsValidator;
import io.ddd4j.data.datascope.annotation.RequiresDataPermissions;
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
        assertThat(validator.isValid("unconfigured", null)).isFalse();
    }

    @Test
    void shouldApplyConsumerProvidedDataScopeDecision() throws Exception {
        DataScopeProvider provider = (dataType, data) -> "tenant".equals(dataType) && "allowed".equals(data);
        Injector injector = Guice.createInjector(new Ddd4jDataScopeJavalinModule(provider));
        RequiresDataPermissionsValidator validator = injector.getInstance(RequiresDataPermissionsValidator.class);
        validator.initialize(Contract.class.getDeclaredField("tenantId")
                .getAnnotation(RequiresDataPermissions.class));

        assertThat(validator.isValid("allowed", null)).isTrue();
        assertThat(validator.isValid("denied", null)).isFalse();
    }

    /**
     * 数据域生效契约断言载体。
     * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
     * @since 7.1.x
     */
    static class Contract {
        @RequiresDataPermissions(dataType = "tenant")
        private String tenantId;
    }
}
