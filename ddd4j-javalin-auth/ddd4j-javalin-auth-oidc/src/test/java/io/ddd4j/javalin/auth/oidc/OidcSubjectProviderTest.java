package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.subject.Subject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OidcSubjectProviderTest {

    @Test
    void shouldExposeVerifiedPrincipalOnlyInsideRequestScope() {
        AuthPrincipal principal = new AuthPrincipal().setLoginId("user-42").setPerms(Set.of("order:read"));
        OidcSubjectProvider provider = new OidcSubjectProvider(token -> principal);
        Subject subject = provider.getSubject();

        assertFalse(subject.isAuthenticated());
        try (OidcSubjectScope ignored = provider.authenticate("signed-token")) {
            assertTrue(subject.isAuthenticated());
            assertSame(principal, subject.getPrincipal());
            assertTrue(subject.isPermitted("order:read"));
        }
        assertFalse(subject.isAuthenticated());
    }

    @Test
    void shouldRejectLocalSessionMutation() {
        Subject subject = new OidcSubjectProvider(token -> new AuthPrincipal()).getSubject();

        assertThrows(UnsupportedOperationException.class, () -> subject.login(new AuthRequest()));
        assertThrows(UnsupportedOperationException.class, subject::logout);
        assertThrows(UnsupportedOperationException.class, subject::refresh);
    }
}
