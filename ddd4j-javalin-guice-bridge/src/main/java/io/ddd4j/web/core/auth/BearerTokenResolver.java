package io.ddd4j.web.core.auth;

import io.ddd4j.kit.lang.StrKit;

import java.util.Optional;

/**
 * 只接受 RFC 6750 标准 Authorization Bearer 头。
 */
public final class BearerTokenResolver {

    private static final String PREFIX = "Bearer ";

    public Optional<String> resolve(String authorization) {
        if (StrKit.isBlank(authorization) || !authorization.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return Optional.empty();
        }
        String token = authorization.substring(PREFIX.length()).trim();
        return StrKit.isBlank(token) ? Optional.empty() : Optional.of(token);
    }
}
