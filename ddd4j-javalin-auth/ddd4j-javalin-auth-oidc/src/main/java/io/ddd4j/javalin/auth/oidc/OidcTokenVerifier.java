package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.auth.AuthPrincipal;

/** 验证 OIDC Bearer Token 并映射为 ddd4j 认证主体。 */
@FunctionalInterface
public interface OidcTokenVerifier {

    /**
     * 验证签名与标准声明。
     *
     * @param token JWT access token
     * @return 只包含白名单声明的认证主体
     */
    AuthPrincipal verify(String token);
}
