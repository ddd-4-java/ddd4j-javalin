package io.ddd4j.web.core.context;

import io.ddd4j.web.core.auth.BearerSubjectAuthenticator.Authentication;

import java.util.Objects;
import java.util.Optional;
import io.ddd4j.web.core.auth.AuthenticationMode;
import io.ddd4j.web.core.auth.BearerSubjectAuthenticator;
import io.ddd4j.web.core.auth.WebAccessPolicy;

/**
 * 在具体 Web 框架之外统一执行访问策略与 Subject 认证。
 */
public final class WebRequestLifecycle {

    private final BearerSubjectAuthenticator authenticator;
    private final WebAccessPolicy accessPolicy;

    public WebRequestLifecycle(BearerSubjectAuthenticator authenticator, WebAccessPolicy accessPolicy) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator must not be null");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy must not be null");
    }

    public Optional<Authentication> authenticate(WebRequestContext context) {
        WebRequestContext requestContext = Objects.requireNonNull(context, "context must not be null");
        AuthenticationMode mode = Objects.requireNonNull(accessPolicy.authenticationMode(requestContext),
                "access policy must return an authentication mode");
        return switch (mode) {
            case DISABLED -> Optional.empty();
            case OPTIONAL -> authenticator.authenticateOptional(requestContext.authorization());
            case REQUIRED -> Optional.of(authenticator.authenticateSubject(requestContext.authorization()));
        };
    }
}
