package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.javalin.auth.AbstractAuthJavalinModule;

import java.util.Objects;

/** 将通用 OIDC 验签器和只读 SubjectProvider 装配进 Guice。 */
public final class Ddd4jOidcJavalinModule extends AbstractAuthJavalinModule {

    private final OidcProperties properties;
    private final OidcTokenVerifier verifier;
    private final OidcSubjectProvider provider;

    public Ddd4jOidcJavalinModule(OidcProperties properties) {
        this(properties, new NimbusOidcTokenVerifier(properties));
    }

    public Ddd4jOidcJavalinModule(OidcProperties properties, OidcTokenVerifier verifier) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.provider = new OidcSubjectProvider(verifier);
    }

    @Override
    protected void configureModule() {
        bind(OidcProperties.class).toInstance(properties);
        bind(OidcTokenVerifier.class).toInstance(verifier);
        bind(OidcSubjectProvider.class).toInstance(provider);
    }

    @Override
    protected SubjectProvider subjectProvider() {
        return provider;
    }
}
