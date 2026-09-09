package io.ddd4j.javalin.auth.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import io.ddd4j.core.auth.AuthPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NimbusOidcTokenVerifierTest {

    private RSAKey rsaKey;
    private HttpServer server;
    private String issuer;
    private NimbusOidcTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] jwks = new JWKSet(rsaKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        server.createContext("/certs", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            exchange.getResponseBody().write(jwks);
            exchange.close();
        });
        server.start();
        issuer = "http://127.0.0.1:" + server.getAddress().getPort() + "/realms/test";
        verifier = new NimbusOidcTokenVerifier(OidcProperties.builder()
                .issuer(issuer)
                .jwksUri(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/certs"))
                .audiences(Set.of("ddd4j-api"))
                .build());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldVerifySignedTokenAndMapAllowlistedClaims() throws Exception {
        AuthPrincipal principal = verifier.verify(token(issuer, "ddd4j-api", Instant.now().plusSeconds(60)));

        assertEquals("user-42", principal.getLoginId());
        assertEquals("user-42", principal.getUserId());
        assertEquals("tenant-7", principal.getOrgId());
        assertEquals(Set.of("order:read"), principal.getPerms());
    }

    @Test
    void shouldRejectWrongIssuerAudienceAndExpiredToken() throws Exception {
        assertThrows(OidcAuthenticationException.class,
                () -> verifier.verify(token("https://evil.example", "ddd4j-api", Instant.now().plusSeconds(60))));
        assertThrows(OidcAuthenticationException.class,
                () -> verifier.verify(token(issuer, "another-api", Instant.now().plusSeconds(60))));
        assertThrows(OidcAuthenticationException.class,
                () -> verifier.verify(token(issuer, "ddd4j-api", Instant.now().minusSeconds(120))));
    }

    private String token(String tokenIssuer, String audience, Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(tokenIssuer)
                .subject("user-42")
                .audience(audience)
                .expirationTime(Date.from(expiresAt))
                .notBeforeTime(Date.from(Instant.now().minusSeconds(5)))
                .claim("tenant_id", "tenant-7")
                .claim("permissions", Set.of("order:read"))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(rsaKey));
        return jwt.serialize();
    }
}
