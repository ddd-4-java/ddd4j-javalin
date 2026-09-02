package io.ddd4j.javalin.auth.shiro;

import io.ddd4j.auth.shiro.subject.ShiroSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.javalin.auth.AbstractAuthJavalinModule;
import io.javalin.Javalin;
import org.apache.shiro.authz.UnauthorizedException;

/**
 * ddd4j-javalin + Apache Shiro Guice 整合模块。
 *
 * <p>对标 ddd4j-auth-spring 的 {@code ShiroAutoConfiguration} + {@code SubjectRegistrar} +
 * {@code ShiroExceptionHandler}，在 Guice 容器中装配 Shiro 鉴权基础设施，承担三个核心职责：
 * <ol>
 *   <li><b>SubjectProvider 注册</b>：绑定 {@link ShiroSubjectProvider} 到 {@link SubjectProvider}
 *       （对标 Spring 的 {@code @Bean SubjectProvider shiroSubjectProvider()}）——
 *       由基类 {@link AbstractAuthJavalinModule} 统一完成 eager 绑定 + {@code SubjectKit} 写回</li>
 *   <li><b>SubjectKit 写回</b>：Injector 创建即把 SubjectProvider 写回 {@code SubjectKit} 静态注册中心，
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
public class Ddd4jShiroJavalinModule extends AbstractAuthJavalinModule {

    /**
     * 注册 Javalin 异常处理器：统一 Shiro 鉴权异常响应（对标 Spring @ControllerAdvice ShiroExceptionHandler）。
     *
     * <p>Javalin 6.x 异常处理 API：{@code app.exception(Class, ExceptionHandler)}。
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
        app.exception(org.apache.shiro.authc.AuthenticationException.class, (exception, ctx) ->
                ctx.status(401).json("{\"code\":\"NOT_AUTHENTICATED\",\"message\":\""
                        + exception.getMessage() + "\"}"));
        app.exception(UnauthorizedException.class, (exception, ctx) ->
                ctx.status(403).json("{\"code\":\"UNAUTHORIZED\",\"message\":\""
                        + exception.getMessage() + "\"}"));
    }

    @Override
    protected SubjectProvider subjectProvider() {
        return new ShiroSubjectProvider();
    }
}
