package io.ddd4j.javalin.sample.auth.satoken;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.util.SubjectKit;
import io.ddd4j.guice.annotation.ddd.ApplicationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权示例控制器：演示 SubjectKit 统一鉴权入口（sa-token 底层，Javalin 编程式路由）。
 *
 * <p>本控制器的业务逻辑与 Spring Boot / Quarkus 示例完全一致，
 * 证明切换底层框架时业务代码零改动（仅路由注册方式不同）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ApplicationService
public class AuthController {

    /**
     * 用 Javalin 编程式 API 注册路由（Javalin 无注解路由，这是原生方式）。
     */
    public void register(Javalin app) {
        app.unsafe.routes.post("/auth/login", this::login);
        app.unsafe.routes.post("/auth/logout", this::logout);
        app.unsafe.routes.get("/auth/me", this::me);
        app.unsafe.routes.get("/auth/check/permission", this::checkPermission);
        app.unsafe.routes.get("/auth/check/role", this::checkRole);
        app.unsafe.routes.get("/auth/status", this::status);
    }

    /**
     * 登录：SubjectKit.login(AuthRequest)
     */
    public void login(Context ctx) {
        String userId = ctx.queryParam("userId");
        AuthPrincipal principal = new AuthPrincipal()
                .setLoginId(userId)
                .setUserId(userId)
                .setRoleCode("user");

        AuthRequest request = AuthRequest.of(userId).setTimeout(7200);
        request.setPrincipal(principal);
        String token = SubjectKit.login(request);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("principal", principal);
        ctx.json(result);
    }

    /**
     * 登出：SubjectKit.logout()
     */
    public void logout(Context ctx) {
        SubjectKit.logout();
        ctx.json(Map.of("success", true));
    }

    /**
     * 当前用户：SubjectKit.getPrincipal()
     */
    public void me(Context ctx) {
        AuthPrincipal principal = SubjectKit.getPrincipal();
        if (principal == null) {
            ctx.json(Map.of("authenticated", false));
            return;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", true);
        result.put("loginId", principal.getLoginId());
        result.put("userId", principal.getUserId());
        result.put("roleCode", principal.getRoleCode());
        ctx.json(result);
    }

    /**
     * 权限校验：SubjectKit.hasPermission()
     */
    public void checkPermission(Context ctx) {
        String permission = ctx.queryParam("permission");
        boolean has = SubjectKit.hasPermission(permission);
        ctx.json(Map.of("permission", permission, "has", has));
    }

    /**
     * 角色校验：SubjectKit.hasRole()
     */
    public void checkRole(Context ctx) {
        String role = ctx.queryParam("role");
        boolean has = SubjectKit.hasRole(role);
        ctx.json(Map.of("role", role, "has", has));
    }

    /**
     * 登录状态：SubjectKit.isLogin()
     */
    public void status(Context ctx) {
        ctx.json(Map.of("login", SubjectKit.isLogin()));
    }

}
