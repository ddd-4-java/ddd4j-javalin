package io.ddd4j.javalin.auth.oidc;

/** 请求级 OIDC 主体作用域，关闭时恢复进入作用域前的身份。 */
@FunctionalInterface
public interface OidcSubjectScope extends AutoCloseable {

    @Override
    void close();
}
