package io.ddd4j.web.core.health;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * Readiness HTTP 响应体，仅暴露整体状态，避免泄露下游依赖信息。
 *
 * 组件 ready：当前应用是否可接收流量
 */
public final class ReadinessResponse {

    private static final long serialVersionUID = 0L;

    private final boolean ready;

    /**
     * 返回探针应接收的 HTTP 状态码。
     *
     * @return 就绪时为 200，未就绪时为 503
     */
    public int httpStatus() {
        return ready ? 200 : 503;
    }

    @JsonCreator()
    public ReadinessResponse(@JsonProperty("ready") boolean ready) {
        this.ready = ready;
    }

    @JsonProperty("ready")
    public boolean ready() {
        return ready;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        ReadinessResponse other = (ReadinessResponse) obj;
        return this.ready == other.ready;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Boolean.hashCode(ready);
        return result;
    }

    @Override
    public String toString() {
        return "ReadinessResponse[ready=" + ready + "]";
    }
}
