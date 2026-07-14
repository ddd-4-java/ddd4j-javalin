package io.ddd4j.javalin.qrcode;

import lombok.Builder;
import lombok.Getter;

/** Explicit Javalin route and resource limits. Routes are disabled by default. */
@Getter
@Builder
public class QrCodeModuleConfig {

    @Builder.Default
    private final boolean routesEnabled = false;

    @Builder.Default
    private final String basePath = "/qrcodes";

    @Builder.Default
    private final int concurrency = Math.min(Runtime.getRuntime().availableProcessors(), 8);

    @Builder.Default
    private final int maxBatchSize = 100;

    @Builder.Default
    private final int maxUploadBytes = 10 * 1024 * 1024;
}
