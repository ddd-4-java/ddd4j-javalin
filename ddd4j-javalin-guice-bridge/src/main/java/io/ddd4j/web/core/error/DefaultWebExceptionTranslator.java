package io.ddd4j.web.core.error;

import io.ddd4j.core.exception.BizRuntimeException;
import io.ddd4j.core.exception.IdempotentException;
import io.ddd4j.core.exception.ParamException;
import io.ddd4j.core.exception.ValidateException;
import io.ddd4j.kit.lang.StrKit;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * ddd4j 默认异常翻译策略。
 */
public final class DefaultWebExceptionTranslator implements WebExceptionTranslator {

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int INTERNAL_SERVER_ERROR = 500;

    @Override
    public WebError translate(Throwable throwable) {
        Throwable cause = Objects.requireNonNull(throwable, "throwable must not be null");
        if (cause instanceof WebStatusException exception) {
            return new WebError(exception.getStatus(), exception.getCode(), message(exception), exception.getData());
        }
        if (cause instanceof IdempotentException) {
            return error(CONFLICT, cause);
        }
        if (cause instanceof ParamException || cause instanceof ValidateException
                || cause instanceof IllegalArgumentException) {
            return error(BAD_REQUEST, cause);
        }
        if (cause instanceof IllegalStateException) {
            return error(CONFLICT, cause);
        }
        if (cause instanceof NoSuchElementException) {
            return error(NOT_FOUND, cause);
        }
        if (cause instanceof SecurityException) {
            return error(FORBIDDEN, cause);
        }
        if (cause instanceof BizRuntimeException exception) {
            int status = normalizeStatus(exception.getCode());
            return new WebError(status, Objects.nonNull(exception.getCode()) ? exception.getCode() : status,
                    message(exception), null);
        }
        return error(INTERNAL_SERVER_ERROR, cause);
    }

    private WebError error(int status, Throwable throwable) {
        return new WebError(status, status, message(throwable), null);
    }

    private String message(Throwable throwable) {
        return StrKit.isBlank(throwable.getMessage()) ? "Internal Server Error" : throwable.getMessage();
    }

    private int normalizeStatus(Integer code) {
        if (Objects.nonNull(code) && code >= BAD_REQUEST && code <= 599) {
            return code;
        }
        return INTERNAL_SERVER_ERROR;
    }
}
