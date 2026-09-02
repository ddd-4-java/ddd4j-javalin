package io.ddd4j.web.core.context;

import io.ddd4j.kit.lang.StrKit;

import java.util.Locale;
import java.util.Objects;

/**
 * HTTP 请求在 ddd4j 内部的框架无关表示。
 */
public record WebRequestContext(
        String requestId,
        String traceId,
        String tenantId,
        String authorization,
        Locale locale,
        String clientIp,
        String method,
        String path) {

    public WebRequestContext {
        requestId = StrKit.isBlank(requestId) ? null : requestId;
        traceId = StrKit.isBlank(traceId) ? requestId : traceId;
        locale = Objects.isNull(locale) ? Locale.getDefault() : locale;
        method = StrKit.isBlank(method) ? null : method.toUpperCase(Locale.ROOT);
        path = StrKit.isBlank(path) ? "/" : path;
    }
}
