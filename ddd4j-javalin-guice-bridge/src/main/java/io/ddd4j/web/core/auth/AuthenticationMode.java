package io.ddd4j.web.core.auth;

/**
 * HTTP 请求的认证模式。
 */
public enum AuthenticationMode {

    /** 不解析也不校验认证信息。 */
    DISABLED,

    /** Bearer Token 可缺省，但携带后必须有效。 */
    OPTIONAL,

    /** 必须携带有效的 Bearer Token。 */
    REQUIRED
}
