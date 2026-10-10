package io.ddd4j.javalin.auth.oidc;

/** OIDC 公钥提供方暂时不可用，调用方应返回不泄露内部细节的 503。 */
public class OidcProviderUnavailableException extends RuntimeException {

    /**
     * 构造 OidcProviderUnavailableException 异常实例。
     * @param message 异常消息。
     * @param cause 根因异常。
     */
    public OidcProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
