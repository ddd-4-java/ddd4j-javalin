package io.ddd4j.javalin.auth.oidc;

/** Token 缺失、格式错误、签名错误或声明不满足信任策略。 */
public class OidcAuthenticationException extends RuntimeException {

    /**
     * 构造 OidcAuthenticationException 异常实例。
     * @param message 异常消息。
     */
    public OidcAuthenticationException(String message) {
        super(message);
    }

    /**
     * 构造 OidcAuthenticationException 异常实例。
     * @param message 异常消息。
     * @param cause 根因异常。
     */
    public OidcAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
