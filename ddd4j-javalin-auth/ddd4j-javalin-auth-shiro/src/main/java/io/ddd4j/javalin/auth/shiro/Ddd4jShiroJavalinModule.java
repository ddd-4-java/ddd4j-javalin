package io.ddd4j.javalin.auth.shiro;

import com.google.inject.AbstractModule;
import io.ddd4j.auth.shiro.subject.ShiroSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import io.javalin.Javalin;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * ddd4j-javalin + Apache Shiro Guice 整合模块。
 *
 * <p>对标 ddd4j-auth-spring 的 {@code ShiroAutoConfiguration} + {@code SubjectRegistrar} +
 * {@code ShiroExceptionHandler}，在 Guice 容器中装配 Shiro 鉴权基础设施，承担三个核心职责：
 * <ol>
 *   <li><b>SubjectProvider 注册</b>：绑定 {@link ShiroSubjectProvider} 到 {@link SubjectProvider}
 *       （对标 Spring 的 {@code @Bean SubjectProvider shiroSubjectProvider()}）</li>
 *   <li><b>SubjectKit 写回</b>：Injector 创建即把 SubjectProvider 写回 {@link SubjectKit} 静态注册中心，
 *       保证 {@code SubjectKit.getSubject()} 全局可用（对标 Spring 的 {@code SubjectRegistrar}
 *       BeanPostProcessor，但用 eager 注册替代）</li>
 *   <li><b>异常处理器</b>：{@link #registerExceptionHandler(Javalin)} 注册 Javalin 异常处理器，
 *       统一 Shiro 异常响应（对标 Spring 的 {@code @ControllerAdvice ShiroExceptionHandler}）</li>
 * </ol>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),                // 核心基础设施
 *     new Ddd4jShiroJavalinModule()          // Shiro 鉴权
 * );
 * // Injector 创建后 SubjectKit 立即可用：
 * Subject subject = SubjectKit.getSubject();
 * // 注册 Javalin 异常处理器（统一 401/403 响应）：
 * Javalin app = Javalin.create();
 * Ddd4jShiroJavalinModule.registerExceptionHandler(app);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jShiroJavalinModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jShiroJavalinModule.class);

    @Override
    protected void configure() {
        // 创建 SubjectProvider 实例并 eager 绑定（单例）
        ShiroSubjectProvider provider = new ShiroSubjectProvider();
        bind(SubjectProvider.class).toInstance(provider);

        // 【关键】对标 Spring SubjectRegistrar（BeanPostProcessor）：
        // Injector 创建即把 SubjectProvider 写回 SubjectKit 静态注册中心，
        // 保证 SubjectKit.getSubject()/login()/isLogin() 等全局可用，无需业务方手动注册。
        SubjectKit.register(provider);
        log.info("ShiroSubjectProvider registered to SubjectKit (eager, at Injector creation)");
    }

    /**
     * 注册 Javalin 异常处理器：统一 Shiro 鉴权异常响应（对标 Spring @ControllerAdvice ShiroExceptionHandler）。
     *
     * <p>Javalin 7.x 异常处理 API：{@code app.unsafe.routes.exception(Class, ExceptionHandler)}。
     * 覆盖三类异常（对齐 Spring 的 ShiroExceptionHandler）：
     * <ul>
     *   <li>401：未认证（AuthenticationException / UnknownAccountException / IncorrectCredentialsException）</li>
     *   <li>403：账号锁定（LockedAccountException）</li>
     *   <li>403：无权限（AuthorizationException / UnauthorizedException）</li>
     * </ul>
     *
     * @param app Javalin 应用实例（需在 start 前调用）
     */
    public static void registerExceptionHandler(Javalin app) {
        // 401：未认证（未登录 / Token 失效 / 账号不存在 / 密码错误）
        registerShiroException(app, AuthenticationException.class, 401, "未登录或登录已过期");
        registerShiroException(app, UnknownAccountException.class, 401, "账号不存在");
        registerShiroException(app, IncorrectCredentialsException.class, 401, "账号或密码错误");

        // 403：账号锁定
        registerShiroException(app, LockedAccountException.class, 403, "账号已被锁定");

        // 403：无权限 / 无角色
        registerShiroException(app, UnauthorizedException.class, 403, "无权限访问");
        registerShiroException(app, AuthorizationException.class, 403, "无权限访问");
    }

    /**
     * 注册单个 Shiro 异常的 Javalin 处理器。
     *
     * @param app             Javalin 应用
     * @param exceptionClass  Shiro 异常类型
     * @param status          HTTP 状态码（401/403）
     * @param message         响应消息
     */
    private static <E extends Exception> void registerShiroException(
            Javalin app, Class<E> exceptionClass, int status, String message) {
        app.unsafe.routes.exception(exceptionClass, (ex, ctx) -> {
            log.warn("Shiro 鉴权异常：{} - {}", exceptionClass.getSimpleName(), ex.getMessage());
            ctx.status(status);
            ctx.json(Map.of("code", status, "msg", message));
        });
    }
}
