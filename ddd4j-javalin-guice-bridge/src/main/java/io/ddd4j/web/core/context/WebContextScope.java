package io.ddd4j.web.core.context;

import io.ddd4j.core.contract.constant.ContextConstants;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.kit.lang.StrKit;
import org.slf4j.MDC;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 请求上下文作用域，关闭时恢复 ThreadContext 与 MDC 的进入前状态。
 */
public final class WebContextScope implements AutoCloseable {

    public static final String REQUEST_ID = "request-id";
    public static final String TRACE_ID = "trace-id";
    public static final String CLIENT_IP = "client-ip";
    public static final String HTTP_METHOD = "http-method";
    public static final String HTTP_PATH = "http-path";

    private final ThreadContextScope threadScope;
    private final Map<String, String> previousMdc;
    private boolean closed;

    private WebContextScope(WebRequestContext context) {
        WebRequestContext requestContext = Objects.requireNonNull(context, "context must not be null");
        this.threadScope = ThreadContextScope.open();
        this.previousMdc = MDC.getCopyOfContextMap();
        bind(requestContext);
    }

    public static WebContextScope open(WebRequestContext context) {
        return new WebContextScope(context);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        threadScope.close();
        if (Objects.isNull(previousMdc)) {
            MDC.clear();
        } else {
            MDC.setContextMap(previousMdc);
        }
        closed = true;
    }

    private void bind(WebRequestContext context) {
        put(REQUEST_ID, context.requestId());
        put(TRACE_ID, context.traceId());
        put(ContextConstants.TENANT_ID, context.tenantId());
        put(ContextConstants.AUTHORIZATION, context.authorization());
        put(ContextConstants.LOCALE, context.locale());
        put(CLIENT_IP, context.clientIp());
        put(HTTP_METHOD, context.method());
        put(HTTP_PATH, context.path());
        putMdc(REQUEST_ID, context.requestId());
        putMdc(TRACE_ID, context.traceId());
        putMdc(ContextConstants.TENANT_ID, context.tenantId());
    }

    private void put(String key, Object value) {
        if (Objects.nonNull(value)) {
            // 1.0.x 改挂：ThreadContext.put → set（1.0.x API 命名）
            ThreadContext.set(key, value);
        }
    }

    private void putMdc(String key, String value) {
        if (StrKit.isNotBlank(value)) {
            MDC.put(key, value);
        } else {
            MDC.remove(key);
        }
    }
}
