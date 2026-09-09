package io.ddd4j.javalin.auth.oidc;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OidcPropertiesTest {

    @Test
    void shouldRequireHttpsForRemoteJwksEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> OidcProperties.builder()
                .issuer("https://id.example.com/realms/main")
                .jwksUri(URI.create("http://id.example.com/realms/main/certs"))
                .audiences(Set.of("ddd4j-api"))
                .build());
    }

    @Test
    void shouldAllowLoopbackHttpForContainerTests() {
        OidcProperties properties = OidcProperties.builder()
                .issuer("http://127.0.0.1:18080/realms/test")
                .jwksUri(URI.create("http://127.0.0.1:18080/realms/test/certs"))
                .audiences(Set.of("ddd4j-api"))
                .clockSkew(Duration.ofSeconds(30))
                .build();

        assertEquals(Set.of("RS256"), properties.getAllowedAlgorithms());
        assertEquals(Duration.ofSeconds(30), properties.getClockSkew());
    }
}
