package io.ddd4j.javalin.auth.oidc;

/**
 * OIDC 公钥提供方暂时不可用，调用方应返回不泄露内部细节的 503。
 */
public class OidcProviderUnavailableException extends RuntimeException {

    /**
     * 构造携带错误描述与原始原因的提供方不可用异常。
     *
     * @param message 错误描述
     * @param cause   原始异常
     */
    public OidcProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
