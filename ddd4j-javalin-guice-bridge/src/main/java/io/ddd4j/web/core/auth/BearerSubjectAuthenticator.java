package io.ddd4j.web.core.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

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

    public final static class Authentication {

        private static final long serialVersionUID = 0L;

        private final String token;

        private final AuthPrincipal principal;

        private final Subject subject;

        @JsonCreator()
        public Authentication(@JsonProperty("token") String token, @JsonProperty("principal") AuthPrincipal principal, @JsonProperty("subject") Subject subject) {
            this.token = token;
            this.principal = principal;
            this.subject = subject;
        }

        @JsonProperty("token")
        public String token() {
            return token;
        }

        @JsonProperty("principal")
        public AuthPrincipal principal() {
            return principal;
        }

        @JsonProperty("subject")
        public Subject subject() {
            return subject;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            Authentication other = (Authentication) obj;
            return Objects.equals(this.token, other.token) && Objects.equals(this.principal, other.principal) && Objects.equals(this.subject, other.subject);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(token);
            result = 31 * result + Objects.hashCode(principal);
            result = 31 * result + Objects.hashCode(subject);
            return result;
        }

        @Override
        public String toString() {
            return "Authentication[token=" + token + ", principal=" + principal + ", subject=" + subject + "]";
        }
    }
}
