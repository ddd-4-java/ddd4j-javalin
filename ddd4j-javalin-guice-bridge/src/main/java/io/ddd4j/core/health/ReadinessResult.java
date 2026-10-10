package io.ddd4j.core.health;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.ddd4j.kit.lang.StrKit;

import java.util.Map;
import java.util.Objects;

/**
 * 单个依赖的就绪检查结果。
 *
 * 组件 name：稳定的依赖标识，例如 {@code postgresql} 或 {@code redis}
 * 组件 ready：是否可接受流量
 * 组件 details：可安全暴露的诊断信息，禁止放入密码、令牌或连接串
 */
public final class ReadinessResult {

    private static final long serialVersionUID = 0L;

    private final String name;

    private final boolean ready;

    private final Map<String, String> details;

    @JsonCreator()
    public ReadinessResult(@JsonProperty("name") String name, @JsonProperty("ready") boolean ready, @JsonProperty("details") Map<String, String> details) {
        if (StrKit.isBlank(name)) {
            throw new IllegalArgumentException("readiness contributor name must not be blank");
        }
        details = Map.copyOf(Objects.requireNonNullElse(details, Map.of()));
        this.name = name;
        this.ready = ready;
        this.details = details;
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
        return new ReadinessResult(name, false, StrKit.isBlank(reason) ? Map.of() : Map.of("reason", reason));
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    @JsonProperty("ready")
    public boolean ready() {
        return ready;
    }

    @JsonProperty("details")
    public Map<String, String> details() {
        return details;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        ReadinessResult other = (ReadinessResult) obj;
        return Objects.equals(this.name, other.name) && this.ready == other.ready && Objects.equals(this.details, other.details);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Boolean.hashCode(ready);
        result = 31 * result + Objects.hashCode(details);
        return result;
    }

    @Override
    public String toString() {
        return "ReadinessResult[name=" + name + ", ready=" + ready + ", details=" + details + "]";
    }
}
