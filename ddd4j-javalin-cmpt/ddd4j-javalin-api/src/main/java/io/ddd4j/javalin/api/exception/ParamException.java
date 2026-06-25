package io.ddd4j.javalin.api.exception;

import io.ddd4j.javalin.api.ApiCode;

@SuppressWarnings("serial")
public class ParamException extends BizRuntimeException {

    public ParamException(ApiCode code, String i18n) {
        super(code, i18n);
    }

    public ParamException(int code, String i18n, String defMsg) {
        super(code, i18n, defMsg);
    }

    public ParamException(int code, String msg, Throwable cause) {
        super(code, msg, cause);
    }

}
