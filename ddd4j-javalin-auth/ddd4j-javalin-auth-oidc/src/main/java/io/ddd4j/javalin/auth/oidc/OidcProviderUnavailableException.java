package io.ddd4j.javalin.auth.oidc;

/**
 * OIDC 公钥提供方暂时不可用，调用方应返回不泄露内部细节的 503。
 */
public class OidcProviderUnavailableException extends RuntimeException {

    /**
     * 创建提供方不可用异常。
     *
     * @param message 不泄露内部细节的失败描述
     * @param cause   底层原因，仅记录日志不对外暴露
     */
    public OidcProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
