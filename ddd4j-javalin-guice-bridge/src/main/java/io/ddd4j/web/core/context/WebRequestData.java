package io.ddd4j.web.core.context;

import java.util.Locale;

/**
 * Web 框架采集到的原始请求元数据。
 */
public record WebRequestData(
        String requestId,
        String traceId,
        String tenantId,
        String authorization,
        Locale locale,
        String forwardedFor,
        String realIp,
        String remoteAddress,
        String method,
        String path) {
}
