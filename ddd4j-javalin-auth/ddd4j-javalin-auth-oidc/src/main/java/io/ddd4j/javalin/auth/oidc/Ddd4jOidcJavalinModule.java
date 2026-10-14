package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.javalin.auth.AbstractAuthJavalinModule;

import java.util.Objects;

/**
 * 将通用 OIDC 验签器和只读 SubjectProvider 装配进 Guice。
 */
public final class Ddd4jOidcJavalinModule extends AbstractAuthJavalinModule {

    private final OidcProperties properties;
    private final OidcTokenVerifier verifier;
    private final OidcSubjectProvider provider;

    /**
     * 构造 Ddd4jOidcJavalinModule Guice 模块实例。
     * @param properties 配置属性。
     */
    public Ddd4jOidcJavalinModule(OidcProperties properties) {
        this(properties, new NimbusOidcTokenVerifier(properties));
    }

    /**
     * 构造 Ddd4jOidcJavalinModule Guice 模块实例。
     * @param properties 配置属性。
     * @param verifier 令牌验证器。
     */
    public Ddd4jOidcJavalinModule(OidcProperties properties, OidcTokenVerifier verifier) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.provider = new OidcSubjectProvider(verifier);
    }

    /**
     * 执行子类自定义绑定装配。
     */
    @Override
    protected void configureModule() {
        bind(OidcProperties.class).toInstance(properties);
        bind(OidcTokenVerifier.class).toInstance(verifier);
        bind(OidcSubjectProvider.class).toInstance(provider);
    }

    /**
     * 返回模块定制的认证主体提供者。
     * @return 认证主体提供者；返回 {@code null} 表示不额外绑定。
     */
    @Override
    protected SubjectProvider subjectProvider() {
        return provider;
    }
}
