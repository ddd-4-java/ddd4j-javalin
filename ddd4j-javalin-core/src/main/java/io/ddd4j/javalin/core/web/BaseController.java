package io.ddd4j.javalin.core.web;

import com.google.inject.Inject;
import io.ddd4j.core.ApiRestResponse;
import io.ddd4j.core.contract.DomainEventPublisher;
import io.ddd4j.core.context.I18nProvider;
import io.javalin.http.Context;

/**
 * Javalin Web 控制器基类（纯净版，零 Spring 依赖）。
 *
 * <p>对标 ddd4j-spring 的 {@code BaseController}，但基于 Javalin {@link Context}：
 * <ul>
 *   <li>响应统一返回 ddd4j-core 的 {@link ApiRestResponse}（不再平行重定义）；</li>
 *   <li>国际化通过 ddd4j-core 的 {@link I18nProvider}（由 Guice 注入）；</li>
 *   <li>异常事件通过 ddd4j-core 的 {@link DomainEventPublisher} 发布。</li>
 * </ul>
 *
 * <p>使用方式（业务 Handler 继承本类，由 ddd4j-javalin-guice 注入依赖）：
 * <pre>{@code
 * public class UserController extends BaseController {
 *     public void me(Context ctx) {
 *         ctx.json(success("user.loaded", "Alice"));
 *     }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public abstract class BaseController {

    @Inject
    protected I18nProvider i18nProvider;

    @Inject
    protected DomainEventPublisher eventPublisher;

    // ======== 响应封装（统一返回 ddd4j-core 的 ApiRestResponse）========

    /**
     * 国际化消息：经 {@link I18nProvider} 解析。
     *
     * @param key  消息 key（或原文）
     * @param args 格式化参数
     * @return 国际化后的消息
     */
    protected String message(String key, Object... args) {
        return i18nProvider.getMessage(key, args);
    }

    protected <T> ApiRestResponse<T> success() {
        return ApiRestResponse.success((T) null);
    }

    protected <T> ApiRestResponse<T> success(T data) {
        return ApiRestResponse.success(data);
    }

    /**
     * 成功响应，消息经 i18n 解析后写入。
     */
    protected <T> ApiRestResponse<T> success(String messageKey, Object... args) {
        return ApiRestResponse.success(message(messageKey, args));
    }

    protected <T> ApiRestResponse<T> fail(String messageKey, Object... args) {
        return ApiRestResponse.fail(message(messageKey, args));
    }

    protected <T> ApiRestResponse<T> error(String messageKey, Object... args) {
        return ApiRestResponse.error(message(messageKey, args));
    }

    /**
     * 将响应写入 Javalin Context（HTTP 200 + JSON）。
     */
    protected <T> void render(Context ctx, ApiRestResponse<T> response) {
        ctx.status(io.javalin.http.HttpStatus.OK).json(response);
    }

}
