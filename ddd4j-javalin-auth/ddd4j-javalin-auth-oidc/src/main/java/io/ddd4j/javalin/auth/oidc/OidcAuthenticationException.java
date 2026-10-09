package io.ddd4j.javalin.auth.oidc;

/** Token 缺失、格式错误、签名错误或声明不满足信任策略。 */
public class OidcAuthenticationException extends RuntimeException {

    /**
     * 创建认证失败异常。
     *
     * @param message 不泄露内部细节的失败描述
     */
    public OidcAuthenticationException(String message) {
        super(message);
    }

    /**
     * 创建携带原因的认证失败异常。
     *
     * @param message 不泄露内部细节的失败描述
     * @param cause   底层原因，仅记录日志不对外暴露
     */
    public OidcAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
