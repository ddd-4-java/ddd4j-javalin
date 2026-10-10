package io.ddd4j.web.core.context;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

import java.util.Locale;

/**
 * Web 框架采集到的原始请求元数据。
 */
public final class WebRequestData {

    private static final long serialVersionUID = 0L;

    private final String requestId;

    private final String traceId;

    private final String tenantId;

    private final String authorization;

    private final Locale locale;

    private final String forwardedFor;

    private final String realIp;

    private final String remoteAddress;

    private final String method;

    private final String path;

    @JsonCreator()
    public WebRequestData(@JsonProperty("requestId") String requestId, @JsonProperty("traceId") String traceId, @JsonProperty("tenantId") String tenantId, @JsonProperty("authorization") String authorization, @JsonProperty("locale") Locale locale, @JsonProperty("forwardedFor") String forwardedFor, @JsonProperty("realIp") String realIp, @JsonProperty("remoteAddress") String remoteAddress, @JsonProperty("method") String method, @JsonProperty("path") String path) {
        this.requestId = requestId;
        this.traceId = traceId;
        this.tenantId = tenantId;
        this.authorization = authorization;
        this.locale = locale;
        this.forwardedFor = forwardedFor;
        this.realIp = realIp;
        this.remoteAddress = remoteAddress;
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

    @JsonProperty("forwardedFor")
    public String forwardedFor() {
        return forwardedFor;
    }

    @JsonProperty("realIp")
    public String realIp() {
        return realIp;
    }

    @JsonProperty("remoteAddress")
    public String remoteAddress() {
        return remoteAddress;
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
        WebRequestData other = (WebRequestData) obj;
        return Objects.equals(this.requestId, other.requestId) && Objects.equals(this.traceId, other.traceId) && Objects.equals(this.tenantId, other.tenantId) && Objects.equals(this.authorization, other.authorization) && Objects.equals(this.locale, other.locale) && Objects.equals(this.forwardedFor, other.forwardedFor) && Objects.equals(this.realIp, other.realIp) && Objects.equals(this.remoteAddress, other.remoteAddress) && Objects.equals(this.method, other.method) && Objects.equals(this.path, other.path);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(requestId);
        result = 31 * result + Objects.hashCode(traceId);
        result = 31 * result + Objects.hashCode(tenantId);
        result = 31 * result + Objects.hashCode(authorization);
        result = 31 * result + Objects.hashCode(locale);
        result = 31 * result + Objects.hashCode(forwardedFor);
        result = 31 * result + Objects.hashCode(realIp);
        result = 31 * result + Objects.hashCode(remoteAddress);
        result = 31 * result + Objects.hashCode(method);
        result = 31 * result + Objects.hashCode(path);
        return result;
    }

    @Override
    public String toString() {
        return "WebRequestData[requestId=" + requestId + ", traceId=" + traceId + ", tenantId=" + tenantId + ", authorization=" + authorization + ", locale=" + locale + ", forwardedFor=" + forwardedFor + ", realIp=" + realIp + ", remoteAddress=" + remoteAddress + ", method=" + method + ", path=" + path + "]";
    }
}
