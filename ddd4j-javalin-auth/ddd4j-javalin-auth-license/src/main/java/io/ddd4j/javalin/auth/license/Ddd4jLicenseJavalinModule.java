package io.ddd4j.javalin.auth.license;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.extension.license.LicenseVerify;
import io.ddd4j.javalin.auth.AbstractAuthJavalinModule;
import lombok.extern.slf4j.Slf4j;

/**
 * Guice Module that wires the ddd4j extension-license {@link LicenseVerify} for Javalin
 * applications.
 *
 * <p>Aligned with ddd4j-boot's {@code DefaultLicenseAutoConfiguration}: a single
 * {@link LicenseVerify} bean is provided, with {@code installLicense} called eagerly at
 * Injector creation and {@code unInstallLicense} registered as a JVM shutdown hook.
 *
 * <p>Note: this module does <em>not</em> call {@code installLicense()} itself — that is
 * the responsibility of the consumer application. The intent is that the ddd4j-web-javalin
 * filter chain (or the consumer's {@code Application} class) inspects the resolved
 * {@code LicenseVerify} and decides whether to gate requests.
 *
 * <p>license 模块无 {@link io.ddd4j.core.subject.SubjectProvider} 可注册（仅提供证书校验），
 * 不覆写 {@code subjectProvider()} 钩子（返回 {@code null}，基类跳过注册）。
 */
@Slf4j
public class Ddd4jLicenseJavalinModule extends AbstractAuthJavalinModule {

    private final LicenseProperties properties;

    public Ddd4jLicenseJavalinModule() {
        this(new LicenseProperties());
    }

    public Ddd4jLicenseJavalinModule(LicenseProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void configureModule() {
        bind(LicenseProperties.class).toInstance(properties);
    }

    @Provides
    @Singleton
    LicenseVerify licenseVerify() {
        LicenseVerify verify = new LicenseVerify(
                properties.getSubject(),
                properties.getPublicAlias(),
                properties.getStorePass(),
                properties.getLicensePath(),
                properties.getPublicKeysStorePath());
        if (properties.isInstallOnStart()) {
            try {
                verify.installLicense();
                log.info("License installed for subject={}", properties.getSubject());
            } catch (RuntimeException e) {
                log.warn("License install failed (will continue without cert): {}", e.getMessage());
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(verify::unInstallLicense,
                "ddd4j-javalin-license-shutdown"));
        return verify;
    }
}