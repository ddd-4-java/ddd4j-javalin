package io.ddd4j.web.javalin;

import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.ddd.event.DomainEventPublisher;
import io.ddd4j.core.i18n.I18nProvider;
import io.ddd4j.web.core.context.WebRequestFailure;
import io.ddd4j.web.javalin.util.WebKit;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

/**
 * Javalin Handler 可选基类，只依赖 ddd4j SPI，不依赖 Guice 或 Spring。
 */
@Slf4j
public abstract class BaseHandler {

    protected String getMessage(String code, Object... args) {
        return Contexts.get(SpiKeys.I18N_PROVIDER, I18nProvider.class)
                .orElse(I18nProvider.DEFAULT)
                .getMessage(code, args);
    }

    protected void logException(Context context, Exception exception) {
        log.error("Exception in request [{} {}]", context.method(), context.path(), exception);
        Contexts.get(SpiKeys.DOMAIN_EVENT_PUBLISHER, DomainEventPublisher.class)
                .ifPresent(publisher -> publisher.publish(
                        new WebRequestFailure(context.method().name(), context.path(), exception)));
    }

    protected String getClientIp(Context context) {
        return WebKit.getClientIp(context);
    }

    protected boolean isAjax(Context context) {
        return WebKit.isAjax(context);
    }
}
