package io.ddd4j.javalin.auth.oidc;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.core.subject.SubjectProvider;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class Ddd4jOidcJavalinModuleTest {

    @Test
    void shouldBindOneSharedVerifierAndSubjectProvider() {
        OidcProperties properties = OidcProperties.builder()
                .issuer("https://id.example.com/realms/main")
                .jwksUri(URI.create("https://id.example.com/realms/main/certs"))
                .audiences(Set.of("ddd4j-api"))
                .build();

        Injector injector = Guice.createInjector(new Ddd4jOidcJavalinModule(properties));

        assertSame(properties, injector.getInstance(OidcProperties.class));
        assertInstanceOf(NimbusOidcTokenVerifier.class, injector.getInstance(OidcTokenVerifier.class));
        assertInstanceOf(OidcSubjectProvider.class, injector.getInstance(SubjectProvider.class));
        assertSame(injector.getInstance(OidcSubjectProvider.class), injector.getInstance(SubjectProvider.class));
    }
}
