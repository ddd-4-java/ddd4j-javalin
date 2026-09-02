package io.ddd4j.web.javalin;

import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.runtime.health.RuntimeReadinessRegistry;
import io.ddd4j.web.core.auth.AuthenticationMode;
import io.ddd4j.web.core.auth.BearerSubjectAuthenticator;
import io.ddd4j.web.core.error.DefaultWebExceptionTranslator;
import io.ddd4j.web.core.auth.PathWebAccessPolicy;
import io.ddd4j.web.core.health.ReadinessEndpoint;
import io.ddd4j.web.core.health.ReadinessResponse;
import io.ddd4j.web.core.context.WebContextScope;
import io.ddd4j.web.core.error.WebError;
import io.ddd4j.web.core.error.WebExceptionTranslator;
import io.ddd4j.web.core.context.WebHeaders;
import io.ddd4j.web.core.idempotency.WebIdempotencyLifecycle;
import io.ddd4j.web.core.observability.WebOtelSupport;
import io.ddd4j.web.core.context.WebRequestContext;
import io.ddd4j.web.core.context.WebRequestContextFactory;
import io.ddd4j.web.core.context.WebRequestData;
import io.ddd4j.web.core.context.WebRequestLifecycle;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 在 Javalin 创建阶段安装统一请求上下文、Bearer Subject、异常与幂等处理链。
 *
 * <p>集成 OTel 分布式追踪：通过 {@link WebOtelSupport} 反射调用 WebOtelIntegration。
 */
@Slf4j
public final class Ddd4jJavalinWeb {

    private static final String STATE_ATTRIBUTE = Ddd4jJavalinWeb.class.getName() + ".state";
    private static final String OTEL_SPAN_ATTR = Ddd4jJavalinWeb.class.getName() + ".otelSpan";

    private final WebRequestContextFactory contextFactory;
    private final WebRequestLifecycle requestLifecycle;
    private final WebExceptionTranslator exceptionTranslator;
    private final Optional<WebIdempotencyLifecycle> idempotencyLifecycle;
    private final ReadinessEndpoint readinessEndpoint;

    public Ddd4jJavalinWeb() {
        this(new WebRequestContextFactory(), new WebRequestLifecycle(new BearerSubjectAuthenticator(),
                        new PathWebAccessPolicy(List.of("/health", "/health/readiness", "/health/liveness",
                                        ReadinessEndpoint.PATH),
                                AuthenticationMode.REQUIRED)),
                new DefaultWebExceptionTranslator(), null, new RuntimeReadinessRegistry());
    }

    /**
     * 使用应用 Runtime 的 registry 注册标准 readiness 端点。
     *
     * @param readinessRegistry 应用 Runtime 管理的就绪状态注册表
     */
    public Ddd4jJavalinWeb(RuntimeReadinessRegistry readinessRegistry) {
        this(new WebRequestContextFactory(), new WebRequestLifecycle(new BearerSubjectAuthenticator(),
                        new PathWebAccessPolicy(List.of("/health", "/health/readiness", "/health/liveness",
                                        ReadinessEndpoint.PATH),
                                AuthenticationMode.REQUIRED)),
                new DefaultWebExceptionTranslator(), null, readinessRegistry);
    }

    public Ddd4jJavalinWeb(WebRequestContextFactory contextFactory, WebRequestLifecycle requestLifecycle,
                           WebExceptionTranslator exceptionTranslator,
                           WebIdempotencyLifecycle idempotencyLifecycle) {
        this(contextFactory, requestLifecycle, exceptionTranslator, idempotencyLifecycle,
                new RuntimeReadinessRegistry());
    }

    public Ddd4jJavalinWeb(WebRequestContextFactory contextFactory, WebRequestLifecycle requestLifecycle,
                           WebExceptionTranslator exceptionTranslator,
                           WebIdempotencyLifecycle idempotencyLifecycle,
                           RuntimeReadinessRegistry readinessRegistry) {
        this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory must not be null");
        this.requestLifecycle = Objects.requireNonNull(requestLifecycle, "requestLifecycle must not be null");
        this.exceptionTranslator = Objects.requireNonNull(exceptionTranslator,
                "exceptionTranslator must not be null");
        this.idempotencyLifecycle = Optional.ofNullable(idempotencyLifecycle);
        RuntimeReadinessRegistry registry = Objects.requireNonNull(readinessRegistry,
                "readinessRegistry must not be null");
        this.readinessEndpoint = new ReadinessEndpoint(() -> registry.readiness().ready());
    }

    public void configure(JavalinConfig config) {
        JavalinConfig javalinConfig = Objects.requireNonNull(config, "config must not be null");
        javalinConfig.routes.before(this::openContext);
        javalinConfig.routes.after(this::completeContext);
        javalinConfig.routes.exception(Exception.class, this::handleException);
        javalinConfig.routes.get(ReadinessEndpoint.PATH, this::readiness);
    }

    private void readiness(Context context) {
        ReadinessResponse response = readinessEndpoint.readiness();
        context.status(response.httpStatus()).json(response);
    }

    private void openContext(Context context) {
        // OTel: 提取上游 TraceContext 并开启 SERVER span
        Map<String, String> headers = extractHeaders(context);
        Object span = WebOtelSupport.startServerSpan(
                context.method().name(), context.path(), headers);
        context.attribute(OTEL_SPAN_ATTR, span);
        WebOtelSupport.activate(span);

        WebRequestContext requestContext = createContext(context);
        RequestState state = new RequestState(WebContextScope.open(requestContext));
        context.attribute(STATE_ATTRIBUTE, state);
        context.header(WebHeaders.REQUEST_ID, requestContext.requestId());
        context.header(WebHeaders.TRACE_ID, requestContext.traceId());
        try {
            requestLifecycle.authenticate(requestContext)
                    .ifPresent(authentication -> ThreadContext.bind(authentication.subject()));
            idempotencyLifecycle.flatMap(lifecycle -> lifecycle.open(requestContext,
                    context.header(WebHeaders.IDEMPOTENCY_KEY))).ifPresent(state::idempotencyScope);
        } catch (RuntimeException exception) {
            WebOtelSupport.recordError(span, exception);
            closeContext(context, false);
            throw exception;
        }
    }

    private void completeContext(Context context) {
        // OTel: 结束 span
        Object span = context.attribute(OTEL_SPAN_ATTR);
        if (Objects.nonNull(span)) {
            WebOtelSupport.endServerSpan(span, context.statusCode());
        }
        closeContext(context, context.statusCode() < 400);
    }

    private void handleException(Exception exception, Context context) {
        WebError error = exceptionTranslator.translate(exception);
        if (error.status() >= 500) {
            log.error("Unhandled Javalin request failure: {} {}", context.method(), context.path(), exception);
        }
        // OTel: 记录异常
        Object span = context.attribute(OTEL_SPAN_ATTR);
        if (Objects.nonNull(span)) {
            WebOtelSupport.recordError(span, exception);
            WebOtelSupport.endServerSpan(span, error.status());
        }
        context.status(error.status()).json(error.toResponse());
        closeContext(context, false);
    }

    private static Map<String, String> extractHeaders(Context context) {
        Map<String, String> headers = new HashMap<>();
        context.headerMap().forEach((k, v) -> {
            if (Objects.nonNull(v)) {
                headers.put(k, v);
            }
        });
        return headers;
    }

    private WebRequestContext createContext(Context context) {
        return contextFactory.create(new WebRequestData(
                context.header(WebHeaders.REQUEST_ID),
                context.header(WebHeaders.TRACE_ID),
                context.header(WebHeaders.TENANT_ID),
                context.header(WebHeaders.AUTHORIZATION),
                resolveLocale(context),
                context.header(WebHeaders.FORWARDED_FOR),
                context.header("X-Real-IP"),
                context.req().getRemoteAddr(),
                context.method().name(),
                context.path()));
    }

    private void closeContext(Context context, boolean successful) {
        RequestState state = context.attribute(STATE_ATTRIBUTE);
        if (Objects.nonNull(state)) {
            state.close(successful);
            context.attribute(STATE_ATTRIBUTE, null);
        }
    }

    private Locale resolveLocale(Context context) {
        String language = context.header("Accept-Language");
        return StrKit.isBlank(language) ? Locale.getDefault() : Locale.forLanguageTag(language.split(",", 2)[0]);
    }

    private static final class RequestState {

        private final WebContextScope contextScope;
        private WebIdempotencyLifecycle.Scope idempotencyScope;

        private RequestState(WebContextScope contextScope) {
            this.contextScope = contextScope;
        }

        private void idempotencyScope(WebIdempotencyLifecycle.Scope scope) {
            this.idempotencyScope = scope;
        }

        private void close(boolean successful) {
            if (Objects.nonNull(idempotencyScope)) {
                if (successful) {
                    idempotencyScope.complete();
                }
                idempotencyScope.close();
            }
            contextScope.close();
        }
    }
}
