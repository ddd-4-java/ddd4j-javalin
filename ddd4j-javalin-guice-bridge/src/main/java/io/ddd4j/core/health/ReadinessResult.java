package io.ddd4j.core.health;

import io.ddd4j.kit.lang.StrKit;

import java.util.Map;
import java.util.Objects;

/**
 * 单个依赖的就绪检查结果。
 *
 * @param name    稳定的依赖标识，例如 {@code postgresql} 或 {@code redis}
 * @param ready   是否可接受流量
 * @param details 可安全暴露的诊断信息，禁止放入密码、令牌或连接串
 */
public record ReadinessResult(String name, boolean ready, Map<String, String> details) {

    public ReadinessResult {
        if (StrKit.isBlank(name)) {
            throw new IllegalArgumentException("readiness contributor name must not be blank");
        }
        details = Map.copyOf(Objects.requireNonNullElse(details, Map.of()));
    }

    /**
     * 创建可接收流量的结果。
     *
     * @param name 依赖标识
     * @return 就绪结果
     */
    public static ReadinessResult ready(String name) {
        return new ReadinessResult(name, true, Map.of());
    }

    /**
     * 创建不可接收流量的结果。
     *
     * @param name   依赖标识
     * @param reason 可安全暴露的失败原因
     * @return 未就绪结果
     */
    public static ReadinessResult unavailable(String name, String reason) {
        return new ReadinessResult(name, false,
                StrKit.isBlank(reason) ? Map.of() : Map.of("reason", reason));
    }
}
