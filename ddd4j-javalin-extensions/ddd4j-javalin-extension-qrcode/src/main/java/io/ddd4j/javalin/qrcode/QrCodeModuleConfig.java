package io.ddd4j.javalin.qrcode;

import lombok.Builder;
import lombok.Getter;

/**
 * Explicit Javalin route and resource limits. Routes are disabled by default.
 */
@Getter
@Builder
public class QrCodeModuleConfig {

    /**
     * 使用全部默认限制构造配置：路由关闭、basePath 为 {@code /qrcodes}、
     * 并发取 CPU 与 8 的较小值、批量 100、上传上限 10 MiB。
     */
    public QrCodeModuleConfig() {
        this(false, "/qrcodes", Math.min(Runtime.getRuntime().availableProcessors(), 8), 100, 10 * 1024 * 1024);
    }

    /**
     * 全参构造：供 Lombok {@code @Builder} 生成的 Builder 调用。
     *
     * @param routesEnabled 是否注册 Javalin 路由
     * @param basePath 路由基础路径
     * @param concurrency 二维码生成并发上限
     * @param maxBatchSize 单次批量生成上限
     * @param maxUploadBytes 解码上传字节上限
     */
    QrCodeModuleConfig(boolean routesEnabled, String basePath, int concurrency, int maxBatchSize, int maxUploadBytes) {
        this.routesEnabled = routesEnabled;
        this.basePath = basePath;
        this.concurrency = concurrency;
        this.maxBatchSize = maxBatchSize;
        this.maxUploadBytes = maxUploadBytes;
    }

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
