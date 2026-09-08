package io.ddd4j.javalin.web;

import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.web.core.context.WebContextScope;
import io.ddd4j.web.core.context.WebHeaders;
import io.ddd4j.web.core.context.WebRequestContext;
import io.ddd4j.web.core.context.WebRequestData;
import io.ddd4j.web.core.context.WebRequestContextFactory;
import io.ddd4j.web.core.context.WebRequestLifecycle;
import io.ddd4j.web.core.error.WebError;
import io.ddd4j.web.core.error.WebExceptionTranslator;
import io.ddd4j.web.core.idempotency.WebIdempotencyLifecycle;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Javalin Web 配置器（6.7.x 本地实现，使用 Javalin 6 API）。
 *
 * <p>替代上游 ddd4j-web-javalin:2.0.x 依赖（该 jar 基于 Javalin 7 编译，
 * 与 Javalin 6.7.0 运行时不兼容）。
 */
@Slf4j
public class Ddd4jJavalinWeb {

    private static final String SCOPE_ATTRIBUTE = Ddd4jJavalinWeb.class.getName() + ".scope";
    private static final String IDEMPOTENCY_SCOPE_ATTRIBUTE =
            Ddd4jJavalinWeb.class.getName() + ".idempotencyScope";

    private final WebRequestContextFactory contextFactory;
    private final WebRequestLifecycle lifecycle;
    private final WebExceptionTranslator translator;
    private final Optional<WebIdempotencyLifecycle> idempotencyLifecycle;

    public Ddd4jJavalinWeb(WebRequestContextFactory contextFactory,
                            WebRequestLifecycle lifecycle,
                            WebExceptionTranslator translator,
                            WebIdempotencyLifecycle idempotencyLifecycle) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.translator = Objects.requireNonNull(translator);
        this.idempotencyLifecycle = Optional.ofNullable(idempotencyLifecycle);
    }

    /**
     * 配置 Javalin 实例（Javalin 6 API）。
     */
    public void configure(JavalinConfig config) {
        config.router.mount(routing -> {
            routing.before(this::openContext);
            routing.after(context -> closeContext(context, context.statusCode() < 400));
            routing.exception(Exception.class, this::handleException);
        });
    }

    private void openContext(Context context) {
        WebRequestContext requestContext = contextFactory.create(new WebRequestData(
                context.header(WebHeaders.REQUEST_ID),
                context.header(WebHeaders.TRACE_ID),
                context.header(WebHeaders.TENANT_ID),
                context.header(WebHeaders.AUTHORIZATION),
                resolveLocale(context),
                context.header(WebHeaders.FORWARDED_FOR),
                context.header("X-Real-IP"),
                context.ip(),
                context.method().name(),
                context.path()));
        WebContextScope scope = WebContextScope.open(requestContext);
        context.attribute(SCOPE_ATTRIBUTE, scope);
        try {
            context.header(WebHeaders.REQUEST_ID, requestContext.requestId());
            if (StrKit.isNotBlank(requestContext.traceId())) {
                context.header(WebHeaders.TRACE_ID, requestContext.traceId());
            }
            lifecycle.authenticate(requestContext)
                    .ifPresent(authentication -> ThreadContext.bind(authentication.subject()));
            idempotencyLifecycle.flatMap(value -> value.open(
                            requestContext, context.header(WebHeaders.IDEMPOTENCY_KEY)))
                    .ifPresent(scopeValue -> context.attribute(IDEMPOTENCY_SCOPE_ATTRIBUTE, scopeValue));
        } catch (RuntimeException exception) {
            closeContext(context, false);
            throw exception;
        }
    }

    private void handleException(Exception exception, Context context) {
        WebError error = translator.translate(exception);
        if (error.status() >= 500) {
            log.error("Unhandled Javalin request failure: {} {}", context.method(), context.path(), exception);
        }
        context.status(error.status()).json(error.toResponse());
        closeContext(context, false);
    }

    private void closeContext(Context context, boolean successful) {
        WebIdempotencyLifecycle.Scope idempotencyScope = context.attribute(IDEMPOTENCY_SCOPE_ATTRIBUTE);
        if (Objects.nonNull(idempotencyScope)) {
            try {
                if (successful) {
                    idempotencyScope.complete();
                }
            } finally {
                idempotencyScope.close();
                context.attribute(IDEMPOTENCY_SCOPE_ATTRIBUTE, null);
            }
        }
        WebContextScope contextScope = context.attribute(SCOPE_ATTRIBUTE);
        if (Objects.nonNull(contextScope)) {
            contextScope.close();
            context.attribute(SCOPE_ATTRIBUTE, null);
        }
    }

    private Locale resolveLocale(Context context) {
        String language = context.header("Accept-Language");
        return StrKit.isBlank(language)
                ? Locale.getDefault()
                : Locale.forLanguageTag(language.split(",", 2)[0]);
    }
}
