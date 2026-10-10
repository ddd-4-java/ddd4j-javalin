package io.ddd4j.web.core.context;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * 可由运行时事件总线观测的框架无关 HTTP 请求失败事件。
 */
public final class WebRequestFailure {

    private static final long serialVersionUID = 0L;

    private final String method;

    private final String path;

    private final Throwable cause;

    @JsonCreator()
    public WebRequestFailure(@JsonProperty("method") String method, @JsonProperty("path") String path, @JsonProperty("cause") Throwable cause) {
        this.method = method;
        this.path = path;
        this.cause = cause;
    }

    @JsonProperty("method")
    public String method() {
        return method;
    }

    @JsonProperty("path")
    public String path() {
        return path;
    }

    @JsonProperty("cause")
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        WebRequestFailure other = (WebRequestFailure) obj;
        return Objects.equals(this.method, other.method) && Objects.equals(this.path, other.path) && Objects.equals(this.cause, other.cause);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(method);
        result = 31 * result + Objects.hashCode(path);
        result = 31 * result + Objects.hashCode(cause);
        return result;
    }

    @Override
    public String toString() {
        return "WebRequestFailure[method=" + method + ", path=" + path + ", cause=" + cause + "]";
    }
}
