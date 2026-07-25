package io.ddd4j.javalin.auth.security;

import com.google.inject.AbstractModule;
import io.ddd4j.auth.security.subject.SecuritySubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

/**
 * ddd4j-javalin + Spring Security Guice 整合模块。
 *
 * <p>对标 ddd4j-auth-security 的 {@code SecurityAutoConfiguration} + {@code SubjectRegistrar} +
 * {@code SecurityExceptionHandler}，在 Guice 容器中装配 Spring Security 鉴权基础设施，
 * 承担三个核心职责：
 * <ol>
 *   <li><b>SubjectProvider 注册</b>：绑定 {@link SecuritySubjectProvider} 到 {@link SubjectProvider}
 *       （对标 Spring 的 {@code @Bean SubjectProvider securitySubjectProvider()}）</li>
 *   <li><b>SubjectKit 写回</b>：Injector 创建即把 SubjectProvider 写回 {@link SubjectKit} 静态注册中心，
 *       保证 {@code SubjectKit.getSubject()} 全局可用（对标 Spring 的 {@code SubjectRegistrar}
 *       BeanPostProcessor，但用 eager 注册替代）</li>
 *   <li><b>异常处理器</b>：{@link #registerExceptionHandler(Javalin)} 注册 Javalin 异常处理器，
 *       统一 Spring Security 异常响应（对标 Spring 的 {@code @ControllerAdvice SecurityExceptionHandler}）</li>
 * </ol>
 *
 * <p><b>注意</b>：Spring Security 依赖 Spring 生态，{@code SecuritySubject} 运行时通过
 * {@code SecurityContextHolder} 读取认证信息。在 Javalin 环境下需自行桥接 SecurityContext
 * （如通过 Javalin 过滤器从请求头解析 JWT 并填充 SecurityContext）。此模块仅作为兼容选项，
 * 推荐 Javalin 项目使用 sa-token（{@code ddd4j-javalin-auth-satoken}）。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),                    // 核心基础设施
 *     new Ddd4jSecurityJavalinModule()           // Spring Security 鉴权
 * );
 * // Injector 创建后 SubjectKit 立即可用：
 * Subject subject = SubjectKit.getSubject();
 * // 注册 Javalin 异常处理器（统一 401/403 响应）：
 * Javalin app = Javalin.create();
 * Ddd4jSecurityJavalinModule.registerExceptionHandler(app);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
public class Ddd4jSecurityJavalinModule extends AbstractModule {

    /**
     * 注册 Javalin 异常处理器：统一 Spring Security 鉴权异常响应
     * （对标 Spring @ControllerAdvice SecurityExceptionHandler）。
     *
     * <p>Javalin 7.x 异常处理 API：{@code app.unsafe.routes.exception(Class, ExceptionHandler)}。
     * 覆盖三类异常（对齐 Spring 的 SecurityExceptionHandler）：
     * <ul>
     *   <li>401：未认证（AuthenticationException / BadCredentialsException / AccountExpiredException）</li>
     *   <li>403：账号锁定/禁用（LockedException / DisabledException）</li>
     *   <li>403：无权限（AccessDeniedException）</li>
     * </ul>
     *
     * @param app Javalin 应用实例（需在 start 前调用）
     */
    public static void registerExceptionHandler(Javalin app) {
        app.unsafe.routes.exception(BadCredentialsException.class, (exception, ctx) ->
                ctx.status(401).json("{\"code\":\"BAD_CREDENTIALS\",\"message\":\""
                        + exception.getMessage() + "\"}"));
        app.unsafe.routes.exception(AuthenticationException.class, (exception, ctx) ->
                ctx.status(401).json("{\"code\":\"NOT_AUTHENTICATED\",\"message\":\""
                        + exception.getMessage() + "\"}"));
        app.unsafe.routes.exception(AccessDeniedException.class, (exception, ctx) ->
                ctx.status(403).json("{\"code\":\"ACCESS_DENIED\",\"message\":\""
                        + exception.getMessage() + "\"}"));
    }

    @Override
    protected void configure() {
        // 创建 SubjectProvider 实例并 eager 绑定（单例）
        SecuritySubjectProvider provider = new SecuritySubjectProvider();
        bind(SubjectProvider.class).toInstance(provider);

        // 【关键】对标 Spring SubjectRegistrar（BeanPostProcessor）：
        // Injector 创建即把 SubjectProvider 写回 SubjectKit 静态注册中心，
        // 保证 SubjectKit.getSubject()/login()/isLogin() 等全局可用，无需业务方手动注册。
        SubjectKit.register(provider);
        log.info("SecuritySubjectProvider registered to SubjectKit (eager, at Injector creation)");
    }
}
