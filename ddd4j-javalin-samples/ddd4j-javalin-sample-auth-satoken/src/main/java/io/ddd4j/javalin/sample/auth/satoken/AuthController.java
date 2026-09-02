package io.ddd4j.javalin.sample.auth.satoken;

import cn.dev33.satoken.stp.StpUtil;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.guice.annotation.ddd.ApplicationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 鉴权示例控制器（sa-token 底层，Javalin 编程式路由）。
 *
 * <p>1.0.x 改挂：2.0.x 的统一鉴权门面 {@code io.ddd4j.core.util.SubjectKit} 与
 * {@code io.ddd4j.core.auth.AuthRequest}（login/logout/session 语义）在 1.0.x core 中不存在，
 * 本示例改用「sa-token 原生登录 + 1.0.x Subject SPI 查询」的等价演示：
 * <ul>
 *   <li>登录/登出/会话：sa-token 原生 {@link StpUtil}</li>
 *   <li>权限/角色校验：经 {@link SubjectProvider} SPI（Guice 注册的 SaTokenSubjectProvider）获取 {@link Subject}</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationService
public class AuthController {

    private final Provider<SubjectProvider> subjectProviders;

    @Inject
    public AuthController(Provider<SubjectProvider> subjectProviders) {
        this.subjectProviders = subjectProviders;
    }

    /**
     * 用 Javalin 编程式 API 注册路由（Javalin 无注解路由，这是原生方式）。
     */
    public void register(Javalin app) {
        app.post("/auth/login", this::login);
        app.post("/auth/logout", this::logout);
        app.get("/auth/me", this::me);
        app.get("/auth/check/permission", this::checkPermission);
        app.get("/auth/check/role", this::checkRole);
        app.get("/auth/status", this::status);
    }

    private Subject subject() {
        SubjectProvider provider = Objects.requireNonNull(subjectProviders).get();
        return provider.getSubject();
    }

    /**
     * 登录：sa-token 原生 StpUtil.login
     */
    public void login(Context ctx) {
        String userId = ctx.queryParam("userId");
        StpUtil.login(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("loginId", StpUtil.getLoginIdAsString());
        ctx.json(result);
    }

    /**
     * 登出：sa-token 原生 StpUtil.logout
     */
    public void logout(Context ctx) {
        StpUtil.logout();
        ctx.json(Map.of("success", true));
    }

    /**
     * 当前用户：sa-token 会话态
     */
    public void me(Context ctx) {
        if (!StpUtil.isLogin()) {
            ctx.json(Map.of("authenticated", false));
            return;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", true);
        result.put("loginId", StpUtil.getLoginIdAsString());
        ctx.json(result);
    }

    /**
     * 权限校验：Subject SPI isPermitted
     */
    public void checkPermission(Context ctx) {
        String permission = ctx.queryParam("permission");
        boolean has = subject().isPermitted(permission);
        ctx.json(Map.of("permission", permission, "has", has));
    }

    /**
     * 角色校验：Subject SPI hasRole
     */
    public void checkRole(Context ctx) {
        String role = ctx.queryParam("role");
        boolean has = subject().hasRole(role);
        ctx.json(Map.of("role", role, "has", has));
    }

    /**
     * 登录状态：sa-token 原生 StpUtil.isLogin
     */
    public void status(Context ctx) {
        ctx.json(Map.of("login", StpUtil.isLogin()));
    }

}
