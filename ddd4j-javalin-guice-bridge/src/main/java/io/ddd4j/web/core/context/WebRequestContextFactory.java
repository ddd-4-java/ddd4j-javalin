package io.ddd4j.web.core.context;

import io.ddd4j.kit.lang.StrKit;

import java.util.Objects;

/**
 * 将各 Web 框架采集的请求数据归一化为 ddd4j 请求上下文。
 */
public final class WebRequestContextFactory {

    private final RequestIdGenerator requestIdGenerator;
    private final ClientIpResolver clientIpResolver;

    public WebRequestContextFactory() {
        this(RequestIdGenerator.uuid(), ClientIpResolver.remoteAddressOnly());
    }

    public WebRequestContextFactory(RequestIdGenerator requestIdGenerator, ClientIpResolver clientIpResolver) {
        this.requestIdGenerator = Objects.requireNonNull(requestIdGenerator, "requestIdGenerator must not be null");
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver, "clientIpResolver must not be null");
    }

    public WebRequestContext create(WebRequestData data) {
        WebRequestData requestData = Objects.requireNonNull(data, "data must not be null");
        String requestId = StrKit.isBlank(requestData.requestId())
                ? requestIdGenerator.generate() : requestData.requestId().trim();
        if (StrKit.isBlank(requestId)) {
            throw new IllegalStateException("requestIdGenerator must return a non-blank request id");
        }
        String clientIp = clientIpResolver.resolve(requestData.forwardedFor(), requestData.realIp(),
                requestData.remoteAddress());
        return new WebRequestContext(requestId, requestData.traceId(), requestData.tenantId(),
                requestData.authorization(), requestData.locale(), clientIp, requestData.method(), requestData.path());
    }
}
