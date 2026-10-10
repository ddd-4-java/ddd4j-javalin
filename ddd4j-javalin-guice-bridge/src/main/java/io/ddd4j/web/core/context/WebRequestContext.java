package io.ddd4j.web.core.context;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.ddd4j.kit.lang.StrKit;

import java.util.Locale;
import java.util.Objects;

/**
 * HTTP 请求在 ddd4j 内部的框架无关表示。
 */
public final class WebRequestContext {

    private static final long serialVersionUID = 0L;

    private final String requestId;

    private final String traceId;

    private final String tenantId;

    private final String authorization;

    private final Locale locale;

    private final String clientIp;

    private final String method;

    private final String path;

    @JsonCreator()
    public WebRequestContext(@JsonProperty("requestId") String requestId, @JsonProperty("traceId") String traceId, @JsonProperty("tenantId") String tenantId, @JsonProperty("authorization") String authorization, @JsonProperty("locale") Locale locale, @JsonProperty("clientIp") String clientIp, @JsonProperty("method") String method, @JsonProperty("path") String path) {
        requestId = StrKit.isBlank(requestId) ? null : requestId;
        traceId = StrKit.isBlank(traceId) ? requestId : traceId;
        locale = Objects.isNull(locale) ? Locale.getDefault() : locale;
        method = StrKit.isBlank(method) ? null : method.toUpperCase(Locale.ROOT);
        path = StrKit.isBlank(path) ? "/" : path;
        this.requestId = requestId;
        this.traceId = traceId;
        this.tenantId = tenantId;
        this.authorization = authorization;
        this.locale = locale;
        this.clientIp = clientIp;
        this.method = method;
        this.path = path;
    }

    @JsonProperty("requestId")
    public String requestId() {
        return requestId;
    }

    @JsonProperty("traceId")
    public String traceId() {
        return traceId;
    }

    @JsonProperty("tenantId")
    public String tenantId() {
        return tenantId;
    }

    @JsonProperty("authorization")
    public String authorization() {
        return authorization;
    }

    @JsonProperty("locale")
    public Locale locale() {
        return locale;
    }

    @JsonProperty("clientIp")
    public String clientIp() {
        return clientIp;
    }

    @JsonProperty("method")
    public String method() {
        return method;
    }

    @JsonProperty("path")
    public String path() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        WebRequestContext other = (WebRequestContext) obj;
        return Objects.equals(this.requestId, other.requestId) && Objects.equals(this.traceId, other.traceId) && Objects.equals(this.tenantId, other.tenantId) && Objects.equals(this.authorization, other.authorization) && Objects.equals(this.locale, other.locale) && Objects.equals(this.clientIp, other.clientIp) && Objects.equals(this.method, other.method) && Objects.equals(this.path, other.path);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(requestId);
        result = 31 * result + Objects.hashCode(traceId);
        result = 31 * result + Objects.hashCode(tenantId);
        result = 31 * result + Objects.hashCode(authorization);
        result = 31 * result + Objects.hashCode(locale);
        result = 31 * result + Objects.hashCode(clientIp);
        result = 31 * result + Objects.hashCode(method);
        result = 31 * result + Objects.hashCode(path);
        return result;
    }

    @Override
    public String toString() {
        return "WebRequestContext[requestId=" + requestId + ", traceId=" + traceId + ", tenantId=" + tenantId + ", authorization=" + authorization + ", locale=" + locale + ", clientIp=" + clientIp + ", method=" + method + ", path=" + path + "]";
    }
}
