package io.ddd4j.javalin.auth.oidc;

/** Token 缺失、格式错误、签名错误或声明不满足信任策略。 */
public class OidcAuthenticationException extends RuntimeException {

    public OidcAuthenticationException(String message) {
        super(message);
    }

    public OidcAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
