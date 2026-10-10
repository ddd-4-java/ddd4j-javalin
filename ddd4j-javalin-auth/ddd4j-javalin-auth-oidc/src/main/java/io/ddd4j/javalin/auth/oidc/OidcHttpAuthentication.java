package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.kit.lang.StrKit;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Javalin 6 的 Bearer Token 请求边界。 */
public final class OidcHttpAuthentication {

    private static final String SCOPE_ATTRIBUTE = OidcHttpAuthentication.class.getName() + ".scope";

    private OidcHttpAuthentication() {
    }

    /**
     * 注册默认拒绝、路径白名单放行的请求认证处理器。
     *
     * @param app         Javalin 应用实例，不允许为 {@code null}
     * @param provider    OIDC 主体提供方，用于校验 Bearer Token，不允许为 {@code null}
     * @param publicPaths 免认证白名单路径集合；为 {@code null} 时按空集合处理
     */
    public static void register(Javalin app, OidcSubjectProvider provider, Set<String> publicPaths) {
        Objects.requireNonNull(app, "app must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Set<String> allowed = Objects.isNull(publicPaths)
                ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(publicPaths));
        app.beforeMatched(context -> authenticate(context, provider, allowed));
        app.afterMatched(OidcHttpAuthentication::closeScope);
    }

    private static void authenticate(Context context, OidcSubjectProvider provider, Set<String> publicPaths) {
        if (publicPaths.contains(context.path())) {
            return;
        }
        String authorization = context.header("Authorization");
        if (StrKit.isBlank(authorization) || !authorization.startsWith("Bearer ")) {
            reject(context, 401, "UNAUTHORIZED");
            return;
        }
        try {
            context.attribute(SCOPE_ATTRIBUTE, provider.authenticate(authorization.substring("Bearer ".length())));
        } catch (OidcProviderUnavailableException exception) {
            reject(context, 503, "OIDC_PROVIDER_UNAVAILABLE");
        } catch (OidcAuthenticationException exception) {
            reject(context, 401, "UNAUTHORIZED");
        }
    }

    private static void closeScope(Context context) {
        OidcSubjectScope scope = context.attribute(SCOPE_ATTRIBUTE);
        if (Objects.nonNull(scope)) {
            scope.close();
        }
    }

    private static void reject(Context context, int status, String code) {
        context.status(status)
                .contentType("application/json")
                .result("{\"code\":\"" + code + "\",\"message\":\"Authentication failed\"}")
                .skipRemainingHandlers();
    }
}
