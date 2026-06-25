package io.javalin.sample.misc;

import cn.hutool.core.util.StrUtil;
import io.javalin.http.Context;
import lombok.Getter;

public class Exchange {
    private final Context context;
    private final static ThreadLocal<Exchange> localContext = new ThreadLocal<>();
    @Getter
    private final String lang;

    public Exchange(Context context) {
        this.context = context;
        var lang = context.header("x-lang");
        if(lang == null) lang = "en";
        this.lang = lang;

        localContext.set(this);
    }

    public static Exchange getLocalContext() {
        return localContext.get();
    }

    public static String getLocalLang() {
        var ctx = localContext.get();
        if (ctx == null) {
           return "en";
        }

        var lang = ctx.lang;
        if(StrUtil.isBlank(lang)) {
            return "en";
        }

        return lang;
    }

    public Context ctx() {
        return context;
    }

    public Exchange json(Object resp) {
        context.json(resp);
        return this;
    }

    public <T> io.javalin.validation.Validator<T> queryParamAsClass(String key, Class<T> tClass) {
        return context.queryParamAsClass(key, tClass);
    }
}
