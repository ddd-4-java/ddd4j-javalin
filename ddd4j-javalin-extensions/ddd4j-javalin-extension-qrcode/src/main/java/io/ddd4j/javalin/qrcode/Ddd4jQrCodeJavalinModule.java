package io.ddd4j.javalin.qrcode;

import com.google.inject.AbstractModule;
import io.ddd4j.extension.qrcode.DefaultQrCodeService;
import io.ddd4j.extension.qrcode.QrCodeService;
import io.ddd4j.extension.qrcode.template.InMemoryQrCodeTemplateRegistry;
import io.ddd4j.extension.qrcode.template.QrCodeTemplateBinder;
import io.ddd4j.extension.qrcode.template.QrCodeTemplateRegistry;
import io.github.hiwepy.zxing.QrCodes;
import io.javalin.Javalin;

import java.util.Objects;

/** Guice assembly for the framework-neutral QR code service. */
public class Ddd4jQrCodeJavalinModule extends AbstractModule implements AutoCloseable {

    private final QrCodeModuleConfig config;
    private final QrCodeService service;
    private final boolean ownsService;

    public Ddd4jQrCodeJavalinModule() {
        this(QrCodeModuleConfig.builder().build());
    }

    public Ddd4jQrCodeJavalinModule(QrCodeModuleConfig config) {
        this(config, new DefaultQrCodeService(QrCodes.encoder(), QrCodes.decoder(),
                config.getConcurrency(), config.getMaxBatchSize()), true);
    }

    public Ddd4jQrCodeJavalinModule(QrCodeModuleConfig config, QrCodeService service) {
        this(config, service, false);
    }

    private Ddd4jQrCodeJavalinModule(QrCodeModuleConfig config, QrCodeService service, boolean ownsService) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.ownsService = ownsService;
    }

    /** Registers routes only when explicitly enabled in the supplied configuration. */
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
