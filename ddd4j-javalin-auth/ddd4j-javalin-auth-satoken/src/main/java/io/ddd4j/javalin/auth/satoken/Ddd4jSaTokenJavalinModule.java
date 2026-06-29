package io.ddd4j.javalin.auth.satoken;

import cn.dev33.satoken.exception.SaTokenException;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.auth.satoken.subject.SaTokenSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import io.javalin.Javalin;

/**
 * ddd4j-javalin + sa-token Guice 整合模块。
 *
 * <p>职责：
 * <ul>
 *   <li>注册 {@link SubjectProvider}（SaTokenSubjectProvider）到 Guice 容器</li>
 *   <li>启动时将 SubjectProvider 写回 {@link SubjectKit} 全局注册中心</li>
 *   <li>注册 Javalin 异常处理器，统一 Sa-Token 异常响应</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>
 * Guice.createInjector(new Ddd4jSaTokenJavalinModule());
 * </pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jSaTokenJavalinModule extends AbstractModule {

    /**
     * 注册 Javalin 异常处理器（统一 Sa-Token 异常响应）。
     *
     * @param app Javalin 应用实例
     */
    public static void registerExceptionHandler(Javalin app) {
        app.unsafe.routes.exception(SaTokenException.class, (ex, ctx) -> {
            ctx.status(401);
            ctx.json(java.util.Map.of(
                    "code", ex.getCode(),
                    "msg", ex.getMessage()
            ));
        });
    }

    /**
     * 提供 SubjectProvider 单例，同时写回 SubjectKit 全局注册中心。
     */
    @Provides
    @Singleton
    public SubjectProvider subjectProvider() {
        SaTokenSubjectProvider provider = new SaTokenSubjectProvider();
        // 写回 SubjectKit 静态注册中心（Javalin 无 BeanPostProcessor，手动注册）
        SubjectKit.register(provider);
        return provider;
    }

}
