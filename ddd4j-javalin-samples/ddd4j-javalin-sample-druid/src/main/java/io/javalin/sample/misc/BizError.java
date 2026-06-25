package io.javalin.sample.misc;

public class BizError extends RuntimeException {
    public final Errors error;
    public final Object[] args;

    public BizError(Errors error, Object... args) {
        super(error.msg);

        this.error = error;
        this.args = args;
    }

    public static BizError of(Errors error, Object... args) {
        return new BizError(error, args);
    }

    public Integer code() {
        return error.code;
    }

    public String message() {
        return error.message(args);
    }

    public String toString() {
        return message();
    }
}
