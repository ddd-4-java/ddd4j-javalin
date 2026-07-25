package io.ddd4j.javalin.auth.license;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.extension.license.LicenseVerify;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link Ddd4jLicenseJavalinModule} resolves a {@link LicenseVerify} bean
 * with the configured subject / path values. We don't actually generate a real {@code .lic}
 * certificate here (that requires keytool) — install is best-effort and warnings are
 * swallowed in the module.
 */
class Ddd4jLicenseJavalinModuleTest {

    @Test
    void shouldResolveLicenseVerifyFromGuice() {
        LicenseProperties props = new LicenseProperties();
        props.setSubject("test-subject");
        props.setPublicAlias("publicAlias");
        props.setStorePass("storePass");
        props.setLicensePath("/tmp/ddd4j-license.lic");
        props.setPublicKeysStorePath("/tmp/ddd4j-publicKeys.store");
        props.setInstallOnStart(false); // unit test: don't try to install

        Injector injector = Guice.createInjector(new Ddd4jLicenseJavalinModule(props));
        LicenseVerify verify = injector.getInstance(LicenseVerify.class);

        assertThat(verify).isNotNull();
        assertThat(injector.getInstance(LicenseProperties.class)).isSameAs(props);
    }
}