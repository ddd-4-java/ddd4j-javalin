package io.ddd4j.web.core.error;

import io.ddd4j.core.api.R;

import java.io.Serializable;

/**
 * HTTP 状态与 ddd4j 响应体之间的统一错误表示。
 */
public record WebError(int status, Serializable code, String message, Object data) {

    public R<Object> toResponse() {
        return R.fail(code, message, data);
    }
}
