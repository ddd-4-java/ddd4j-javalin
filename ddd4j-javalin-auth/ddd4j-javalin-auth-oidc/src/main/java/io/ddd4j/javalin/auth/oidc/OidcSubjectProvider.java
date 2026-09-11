package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;

import java.util.Objects;

/**
 * 为 Javalin 请求提供只读 OIDC Subject。
 *
 * <p>身份状态通过 {@link ThreadContext#SUBJECT_KEY}（TTL 透传）持有。
 * 修改前：使用普通 {@code ThreadLocal<AuthPrincipal>}；异步消费（MQ / 定时任务 /
 * 虚拟线程切换）时 broker worker 线程拿不到身份，造成 {@code SubjectKit.getSubject()}
 * 在异步路径返回 {@code null}。修复为 TTL 后，{@code ThreadContext.getSubject()} 在
 * 同一 TTL 池内的异步线程可读。</p>
 *
 * <p>{@link OidcSubject} 通过 {@link ThreadContext} 反查主体身份；本类不持有
 * 任何 {@code ThreadLocal}。</p>
 */
public final class OidcSubjectProvider implements SubjectProvider {

    private final OidcSubject subject;
    private final OidcTokenVerifier verifier;

    public OidcSubjectProvider(OidcTokenVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.subject = new OidcSubject(verifier);
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    /**
     * 验证 token 并在当前线程建立请求级身份作用域（写入 {@link ThreadContext}）。
     *
     * <p>作用域关闭时恢复进入作用域前 {@link ThreadContext#SUBJECT_KEY} 的旧值；
     * 若旧值为 {@code null} 则调用 {@link ThreadContext#unbindSubject()} 清空槽位，
     * 避免请求间身份串扰。</p>
     *
     * @param token OIDC ID Token（不含 {@code Bearer } 前缀）
     * @return 请求级作用域，{@link OidcSubjectScope#close()} 必须调用（推荐 try-with-resources）
     */
    public OidcSubjectScope authenticate(String token) {
        AuthPrincipal verified = verifier.verify(token);
        Subject previousBound = ThreadContext.getSubject();
        ThreadContext.bind(new OidcSubjectDelegate(verified, verifier));
        return () -> {
            if (Objects.isNull(previousBound)) {
                ThreadContext.unbindSubject();
            } else {
                ThreadContext.bind(previousBound);
            }
        };
    }
}