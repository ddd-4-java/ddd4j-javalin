package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.core.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OidcSubjectProviderTest {

    @AfterEach
    void clearContext() {
        ThreadContext.clear();
    }

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

    /**
     * 关键回归断言：{@link OidcSubjectProvider#authenticate(String)} 写入
     * {@link ThreadContext#SUBJECT_KEY}，{@code ThreadContext.getSubject()}
     * 在认证作用域内可读。
     *
     * <p>修复前：{@code OidcSubjectProvider} 持有自有普通 {@code ThreadLocal}，
     * 此断言会失败。</p>
     */
    @Test
    void shouldExposeSubjectViaThreadContextInsideRequestScope() {
        AuthPrincipal principal = new AuthPrincipal().setLoginId("user-ctx");
        OidcSubjectProvider provider = new OidcSubjectProvider(token -> principal);

        assertNull(ThreadContext.getSubject(), "no Subject before authenticate()");

        try (OidcSubjectScope ignored = provider.authenticate("signed-token")) {
            Subject bound = ThreadContext.getSubject();
            assertNotNull(bound, "authenticate() must bind Subject to ThreadContext");
            // ThreadContext 槽位里必须是 OidcSubjectDelegate（typed identifier），
            // 避免污染其它 Subject 实现写入的槽位。
            assertTrue(bound instanceof OidcSubjectDelegate,
                    "ThreadContext SUBJECT_KEY must hold OidcSubjectDelegate, but was " + bound.getClass().getName());
            assertTrue(bound.isAuthenticated());
            assertSame(principal, bound.getPrincipal());
            assertTrue(provider.getSubject().isAuthenticated());
        }

        assertNull(ThreadContext.getSubject(), "scope close() must restore Subject to null");
        assertFalse(provider.getSubject().isAuthenticated());
    }

    /**
     * 嵌套作用域：内层 close 后应恢复到外层主体。
     */
    @Test
    void shouldRestorePreviousSubjectOnNestedScopeClose() {
        AuthPrincipal outer = new AuthPrincipal().setLoginId("user-outer");
        AuthPrincipal inner = new AuthPrincipal().setLoginId("user-inner");
        OidcSubjectProvider provider = new OidcSubjectProvider(token -> {
            // 用一个能区分 outer/inner 的简单逻辑：真实场景下 verifier 不会这样用，
            // 这里只为了断言"闭包后回滚到上一次 bind"
            return token.contains("inner") ? inner : outer;
        });

        try (OidcSubjectScope outerScope = provider.authenticate("outer-token")) {
            assertSame(outer, ThreadContext.getSubject().getPrincipal());
            try (OidcSubjectScope innerScope = provider.authenticate("inner-token")) {
                assertSame(inner, ThreadContext.getSubject().getPrincipal());
            }
            // inner close 后回滚
            assertSame(outer, ThreadContext.getSubject().getPrincipal());
        }
        assertNull(ThreadContext.getSubject());
    }

    /**
     * 同一线程多次调 {@link ThreadContext#getSubject()} 仍可读（即
     * {@link io.ddd4j.core.context.ThreadContext} 透传语义不被本认证作用域破坏）。
     */
    @Test
    void shouldKeepSubjectVisibleThroughoutRequestScope() {
        AuthPrincipal principal = new AuthPrincipal().setLoginId("user-vis");
        OidcSubjectProvider provider = new OidcSubjectProvider(token -> principal);

        try (OidcSubjectScope ignored = provider.authenticate("signed-token")) {
            for (int i = 0; i < 3; i++) {
                Subject bound = ThreadContext.getSubject();
                assertNotNull(bound, "iteration " + i);
                assertSame(principal, bound.getPrincipal());
                assertTrue(bound.isAuthenticated());
                assertTrue(provider.getSubject().isAuthenticated());
            }
        }
        assertNull(ThreadContext.getSubject());
    }
}
