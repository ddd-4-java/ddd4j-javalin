package io.javalin.sample.misc;

public enum Errors {

    OK(0, "ok"),
    USER_NOT_EXISTS(10001, "user not exists"),
    ;

    public final String msg;
    public final int code;

    Errors(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String message(Object... args) {
        return String.format(msg, args);
    }

    public BizError exception(Object... args) {
        return new BizError(this, args);
    }

    public String toString() {
        return message();
    }
}
