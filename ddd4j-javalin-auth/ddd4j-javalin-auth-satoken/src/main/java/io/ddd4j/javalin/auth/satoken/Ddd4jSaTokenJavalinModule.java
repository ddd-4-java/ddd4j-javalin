package io.ddd4j.javalin.auth.satoken;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import com.google.inject.AbstractModule;
import io.ddd4j.auth.satoken.handler.SaInternalCheckHandler;
import io.ddd4j.auth.satoken.handler.SaMixCheckLoginHandler;
import io.ddd4j.auth.satoken.subject.SaTokenSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;

/**
 * ddd4j-javalin + sa-token Guice 整合模块。
 *
 * <p>对标 ddd4j-auth-spring 的 {@code SaTokenAutoConfiguration} + {@code SubjectRegistrar}，
 * 在 Guice 容器中装配 sa-token 鉴权基础设施，承担三个核心职责：
 * <ol>
 *   <li><b>SubjectProvider 注册</b>：绑定 {@link SaTokenSubjectProvider} 到 {@link SubjectProvider}
 *       （对标 Spring 的 {@code @Bean SubjectProvider saTokenSubjectProvider()}）</li>
 *   <li><b>Sa-Token 注解处理器注册</b>：注册 ddd4j 扩展的混合登录和内部 API Key 注解处理器</li>
 *   <li><b>SubjectKit 写回</b>：Injector 创建即把 SubjectProvider 写回 {@link SubjectKit} 静态注册中心，
 *       保证 {@code SubjectKit.getSubject()} 全局可用（对标 Spring 的 {@code SubjectRegistrar}
 *       BeanPostProcessor，但用 eager 注册替代，无需 BeanPostProcessor）</li>
 *   <li><b>异常处理器</b>：{@link #registerExceptionHandler(Javalin)} 注册 Javalin 异常处理器，
 *       统一 Sa-Token 鉴权异常响应（对标 Spring 的 {@code @ControllerAdvice SaTokenExceptionHandler}）</li>
 * </ol>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * Injector injector = Guice.createInjector(
 *     new Ddd4jGuiceModule(),                // 核心基础设施（SubjectKit 等）
 *     new Ddd4jSaTokenJavalinModule()        // sa-token 鉴权
 * );
 * // Injector 创建后 SubjectKit 立即可用：
 * Subject subject = SubjectKit.getSubject();
 * // 注册 Javalin 异常处理器（统一 401 响应）：
 * Javalin app = Javalin.create();
 * Ddd4jSaTokenJavalinModule.registerExceptionHandler(app);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
public class Ddd4jSaTokenJavalinModule extends AbstractModule {

    /**
     * 注册 Javalin 异常处理器：统一 Sa-Token 鉴权异常响应（对标 Spring @ControllerAdvice）。
     *
     * <p>Javalin 6.x 的异常处理 API：{@code app.exception(Class, ExceptionHandler)}。
     * 返回 401 + JSON body（对齐 Spring 的 {@code SaTokenExceptionHandler} 响应结构）。
     *
     * @param app Javalin 应用实例（需在 start 前调用）
     */
    public static void registerExceptionHandler(Javalin app) {
        app.exception(NotLoginException.class, (exception, ctx) ->
                ctx.status(401).json("{\"code\":\"NOT_LOGIN\",\"message\":\""
                        + exception.getMessage() + "\"}"));
        app.exception(NotPermissionException.class, (exception, ctx) ->
                ctx.status(403).json("{\"code\":\"NOT_PERMISSION\",\"message\":\""
                        + exception.getMessage() + "\"}"));
        app.exception(NotRoleException.class, (exception, ctx) ->
                ctx.status(403).json("{\"code\":\"NOT_ROLE\",\"message\":\""
                        + exception.getMessage() + "\"}"));
    }

    @Override
    protected void configure() {
        // 创建 SubjectProvider 实例并 eager 绑定（单例）
        SaTokenSubjectProvider provider = new SaTokenSubjectProvider();
        SaMixCheckLoginHandler mixCheckLoginHandler = new SaMixCheckLoginHandler();
        SaInternalCheckHandler internalCheckHandler = new SaInternalCheckHandler();
        bind(SubjectProvider.class).toInstance(provider);
        bind(SaMixCheckLoginHandler.class).toInstance(mixCheckLoginHandler);
        bind(SaInternalCheckHandler.class).toInstance(internalCheckHandler);

        SaAnnotationStrategy.instance.registerAnnotationHandler(mixCheckLoginHandler);
        SaAnnotationStrategy.instance.registerAnnotationHandler(internalCheckHandler);

        // 【关键】对标 Spring SubjectRegistrar（BeanPostProcessor）：
        // Injector 创建即把 SubjectProvider 写回 SubjectKit 静态注册中心，
        // 保证 SubjectKit.getSubject()/login()/isLogin() 等全局可用，无需业务方手动注册。
        SubjectKit.register(provider);
        log.info("SaTokenSubjectProvider registered to SubjectKit (eager, at Injector creation)");
    }
}
