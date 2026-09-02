package io.ddd4j.web.core.error;

import java.io.Serializable;

/**
 * Web 适配层可移植的 HTTP 状态异常。
 */
public class WebStatusException extends RuntimeException {

    private final int status;
    private final Serializable code;
    private final Object data;

    public WebStatusException(int status, String message) {
        this(status, status, message, null);
    }

    public WebStatusException(int status, Serializable code, String message, Object data) {
        super(message);
        this.status = status;
        this.code = code;
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public Serializable getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }
}
