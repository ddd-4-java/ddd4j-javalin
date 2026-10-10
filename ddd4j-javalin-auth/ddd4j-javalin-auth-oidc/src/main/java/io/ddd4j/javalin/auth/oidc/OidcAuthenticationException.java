package io.ddd4j.javalin.auth.oidc;

/**
 * Token 缺失、格式错误、签名错误或声明不满足信任策略。
 */
public class OidcAuthenticationException extends RuntimeException {

    /**
     * 构造仅携带错误描述的认证异常。
     *
     * @param message 错误描述
     */
    public OidcAuthenticationException(String message) {
        super(message);
    }

    /**
     * 构造携带错误描述与原始原因的认证异常。
     *
     * @param message 错误描述
     * @param cause   原始异常
     */
    public OidcAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
