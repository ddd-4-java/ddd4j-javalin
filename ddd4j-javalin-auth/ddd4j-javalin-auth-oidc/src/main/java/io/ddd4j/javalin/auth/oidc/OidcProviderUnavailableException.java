package io.ddd4j.javalin.auth.oidc;

/** OIDC 公钥提供方暂时不可用，调用方应返回不泄露内部细节的 503。 */
public class OidcProviderUnavailableException extends RuntimeException {

    public OidcProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
