package io.javalin.sample.misc;

import java.util.HashMap;

public class R extends HashMap<String, Object> {
    public static R ok(Object data) {
        return new R().data(data).code(200).message("success");
    }

    public static R error(int code) {
        return new R().code(code);
    }

    public static R error(Errors errors, Object... args) {
        return error(errors.code).message(errors.message(args));
    }

    public R code(int code) {
        put("code", code);
        return this;
    }

    public R message(String msg) {
        put("message", msg);
        return this;
    }

    public R data(Object data) {
        put("data", data);
        return this;
    }

    public int code() {
        return (int) get("code");
    }

    public String message() {
        return (String) get("message");
    }

    public Object data() {
        return get("data");
    }

    public R set(String key, Object val) {
        this.put(key, val);
        return this;
    }
}
