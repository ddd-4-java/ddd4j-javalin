package io.ddd4j.web.core.auth;

import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;

import java.util.Objects;
import java.util.Optional;
import io.ddd4j.web.core.error.WebStatusException;

/**
 * 将标准 Bearer Token 委托给当前运行时注册的 Subject SPI。
 */
public final class BearerSubjectAuthenticator {

    private final BearerTokenResolver tokenResolver;

    public BearerSubjectAuthenticator() {
        this(new BearerTokenResolver());
    }

    public BearerSubjectAuthenticator(BearerTokenResolver tokenResolver) {
        this.tokenResolver = Objects.requireNonNull(tokenResolver, "tokenResolver must not be null");
    }

    public AuthPrincipal authenticate(String authorization) {
        return authenticateSubject(authorization).principal();
    }

    public Authentication authenticateSubject(String authorization) {
        String token = tokenResolver.resolve(authorization)
                .orElseThrow(() -> new WebStatusException(401, "Bearer token is required"));
        return authenticateToken(token);
    }

    public Optional<Authentication> authenticateOptional(String authorization) {
        return tokenResolver.resolve(authorization).map(this::authenticateToken);
    }

    private Authentication authenticateToken(String token) {
        SubjectProvider provider = Contexts.get(SpiKeys.SUBJECT_PROVIDER, SubjectProvider.class)
                .orElseThrow(() -> new WebStatusException(401, "Subject provider is unavailable"));
        Subject subject = provider.getSubject();
        AuthPrincipal principal = subject.verify(token);
        if (Objects.isNull(principal)) {
            throw new WebStatusException(401, "Bearer token is invalid or expired");
        }
        return new Authentication(token, principal, subject);
    }

    public record Authentication(String token, AuthPrincipal principal, Subject subject) {
    }
}
