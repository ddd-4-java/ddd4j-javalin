package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;

import java.util.Objects;

/** 为 Javalin 请求提供只读 OIDC Subject。 */
public final class OidcSubjectProvider implements SubjectProvider {

    private final ThreadLocal<AuthPrincipal> current = new ThreadLocal<>();
    private final OidcSubject subject;
    private final OidcTokenVerifier verifier;

    public OidcSubjectProvider(OidcTokenVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.subject = new OidcSubject(current, verifier);
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    /** 验证 token 并在当前线程建立请求级身份作用域。 */
    public OidcSubjectScope authenticate(String token) {
        AuthPrincipal previous = current.get();
        current.set(verifier.verify(token));
        return () -> {
            if (Objects.isNull(previous)) {
                current.remove();
            } else {
                current.set(previous);
            }
        };
    }
}
