package io.ddd4j.javalin.auth.shiro;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.auth.shiro.subject.ShiroSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import io.javalin.Javalin;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;

/**
 * ddd4j-javalin + Apache Shiro Guice 整合模块。
 *
 * <p>职责：
 * <ul>
 *   <li>注册 {@link SubjectProvider}（ShiroSubjectProvider）到 Guice 容器</li>
 *   <li>启动时将 SubjectProvider 写回 {@link SubjectKit} 全局注册中心</li>
 *   <li>注册 Javalin 异常处理器，统一 Shiro 异常响应</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jShiroJavalinModule extends AbstractModule {

    /**
     * 注册 Javalin 异常处理器（统一 Shiro 异常响应）。
     */
    public static void registerExceptionHandler(Javalin app) {
        app.unsafe.routes.exception(AuthenticationException.class, (ex, ctx) -> {
            ctx.status(401);
            ctx.json(java.util.Map.of("code", 401, "msg", "未登录或登录已过期"));
        });
        app.unsafe.routes.exception(AuthorizationException.class, (ex, ctx) -> {
            ctx.status(403);
            ctx.json(java.util.Map.of("code", 403, "msg", "无权限访问"));
        });
    }

    @Provides
    @Singleton
    public SubjectProvider subjectProvider() {
        ShiroSubjectProvider provider = new ShiroSubjectProvider();
        SubjectKit.register(provider);
        return provider;
    }

}
