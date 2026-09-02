package io.ddd4j.web.javalin.util;

import io.ddd4j.kit.lang.StrKit;
import io.javalin.http.Context;

import java.util.Objects;

/**
 * Javalin 请求工具。
 */
public final class WebKit {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
    };

    private WebKit() {
    }

    public static String getClientIp(Context context) {
        String ip = null;
        for (String header : IP_HEADER_CANDIDATES) {
            ip = context.header(header);
            if (StrKit.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }
        if (StrKit.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = context.ip();
        }
        if (Objects.nonNull(ip) && ip.contains(",")) {
            return ip.split(",", 2)[0].trim();
        }
        return ip;
    }

    public static boolean isAjax(Context context) {
        return "XMLHttpRequest".equalsIgnoreCase(context.header("X-Requested-With"));
    }
}
