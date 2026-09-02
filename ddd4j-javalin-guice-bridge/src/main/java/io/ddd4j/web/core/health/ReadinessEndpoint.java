package io.ddd4j.web.core.health;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * 将任意就绪状态转换为不包含下游诊断信息的 Web 响应。
 */
public final class ReadinessEndpoint {

    /** Kubernetes 等上游探针使用的显式就绪路径。 */
    public static final String PATH = "/-/ready";

    private final BooleanSupplier readinessSupplier;

    public ReadinessEndpoint(BooleanSupplier readinessSupplier) {
        this.readinessSupplier = Objects.requireNonNull(readinessSupplier,
                "readinessSupplier must not be null");
    }

    /**
     * 查询当前运行时是否可接收流量。
     *
     * @return 仅包含整体就绪状态的安全响应
     */
    public ReadinessResponse readiness() {
        return new ReadinessResponse(readinessSupplier.getAsBoolean());
    }
}
