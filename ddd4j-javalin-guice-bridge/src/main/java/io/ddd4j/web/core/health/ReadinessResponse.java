package io.ddd4j.web.core.health;

/**
 * Readiness HTTP 响应体，仅暴露整体状态，避免泄露下游依赖信息。
 *
 * @param ready 当前应用是否可接收流量
 */
public record ReadinessResponse(boolean ready) {

    /**
     * 返回探针应接收的 HTTP 状态码。
     *
     * @return 就绪时为 200，未就绪时为 503
     */
    public int httpStatus() {
        return ready ? 200 : 503;
    }
}
