package io.ddd4j.javalin.qrcode;

import com.google.inject.AbstractModule;
import io.ddd4j.extension.qrcode.DefaultQrCodeService;
import io.ddd4j.extension.qrcode.QrCodeService;
import io.ddd4j.extension.qrcode.template.InMemoryQrCodeTemplateRegistry;
import io.ddd4j.extension.qrcode.template.QrCodeTemplateBinder;
import io.ddd4j.extension.qrcode.template.QrCodeTemplateRegistry;
import io.javalin.Javalin;

import java.util.Objects;

/** Guice assembly for the framework-neutral QR code service. */
public class Ddd4jQrCodeJavalinModule extends AbstractModule implements AutoCloseable {

    private final QrCodeModuleConfig config;
    private final QrCodeService service;
    private final boolean ownsService;

    /**
     * 以默认配置与默认 {@link DefaultQrCodeService} 构造模块，并负责关闭服务。
     */
    public Ddd4jQrCodeJavalinModule() {
        this(QrCodeModuleConfig.builder().build());
    }

    /**
     * 以指定配置构造模块，并按配置参数创建 {@link DefaultQrCodeService}，模块负责关闭该服务。
     *
     * @param config 模块配置，不可为 {@code null}
     */
    public Ddd4jQrCodeJavalinModule(QrCodeModuleConfig config) {
        this(config, new DefaultQrCodeService(config.getConcurrency(), config.getMaxBatchSize()), true);
    }

    /**
     * 以指定配置与外部提供的服务构造模块；服务由调用方持有，模块关闭时不销毁它。
     *
     * @param config 模块配置，不可为 {@code null}
     * @param service 外部提供的 {@link QrCodeService}，不可为 {@code null}
     */
    public Ddd4jQrCodeJavalinModule(QrCodeModuleConfig config, QrCodeService service) {
        this(config, service, false);
    }

    /**
     * 全参构造，统一做非空校验。
     *
     * @param config 模块配置，不可为 {@code null}
     * @param service 二维码服务，不可为 {@code null}
     * @param ownsService 为 {@code true} 时由本模块在 {@link #close()} 中销毁服务
     * @throws NullPointerException 当 {@code config} 或 {@code service} 为 {@code null}
     */
    private Ddd4jQrCodeJavalinModule(QrCodeModuleConfig config, QrCodeService service, boolean ownsService) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.ownsService = ownsService;
    }

    /**
     * Registers routes only when explicitly enabled in the supplied configuration.
     *
     * @param app 目标 Javalin 实例
     */
    public void registerRoutes(Javalin app) {
        if (config.isRoutesEnabled()) {
            new QrCodeRoutes(service, config).register(app);
        }
    }

    @Override
    protected void configure() {
        bind(QrCodeModuleConfig.class).toInstance(config);
        bind(QrCodeService.class).toInstance(service);
        bind(QrCodeTemplateRegistry.class).to(InMemoryQrCodeTemplateRegistry.class);
        bind(QrCodeTemplateBinder.class);
    }

    @Override
    public void close() throws Exception {
        if (ownsService && service instanceof AutoCloseable) {
            ((AutoCloseable) service).close();
        }
    }
}
