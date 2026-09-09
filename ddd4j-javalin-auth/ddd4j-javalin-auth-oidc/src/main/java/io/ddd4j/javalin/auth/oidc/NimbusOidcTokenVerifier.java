package io.ddd4j.javalin.auth.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.kit.lang.StrKit;

import java.text.ParseException;
import java.time.Instant;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 基于 Nimbus JOSE + JWT 的 OIDC resource-server 验签器。 */
public final class NimbusOidcTokenVerifier implements OidcTokenVerifier {

    private final OidcProperties properties;
    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public NimbusOidcTokenVerifier(OidcProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        try {
            JWKSource<SecurityContext> source = JWKSourceBuilder.<SecurityContext>create(
                    properties.getJwksUri().toURL()).retrying(true).build();
            DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            Set<JWSAlgorithm> algorithms = new HashSet<>();
            for (String algorithm : properties.getAllowedAlgorithms()) {
                algorithms.add(JWSAlgorithm.parse(algorithm));
            }
            jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(algorithms, source));
            this.processor = jwtProcessor;
        } catch (Exception exception) {
            throw new IllegalArgumentException("jwksUri is invalid", exception);
        }
    }

    @Override
    public AuthPrincipal verify(String token) {
        if (StrKit.isBlank(token)) {
            throw new OidcAuthenticationException("Bearer token is required");
        }
        try {
            JWTClaimsSet claims = processor.process(token, null);
            validateClaims(claims);
            return mapPrincipal(claims);
        } catch (com.nimbusds.jose.RemoteKeySourceException exception) {
            throw new OidcProviderUnavailableException("OIDC provider is unavailable", exception);
        } catch (Exception exception) {
            throw new OidcAuthenticationException("Bearer token is invalid", exception);
        }
    }

    private void validateClaims(JWTClaimsSet claims) throws ParseException {
        if (!properties.getIssuer().equals(claims.getIssuer())) {
            throw new OidcAuthenticationException("Bearer token is invalid");
        }
        List<String> audience = claims.getAudience();
        if (Objects.isNull(audience) || audience.stream().noneMatch(properties.getAudiences()::contains)) {
            throw new OidcAuthenticationException("Bearer token is invalid");
        }
        Instant now = Instant.now();
        Instant skewedPast = now.minus(properties.getClockSkew());
        Instant skewedFuture = now.plus(properties.getClockSkew());
        Date expiresAt = claims.getExpirationTime();
        Date notBefore = claims.getNotBeforeTime();
        if (Objects.isNull(expiresAt) || expiresAt.toInstant().isBefore(skewedPast)
                || Objects.nonNull(notBefore) && notBefore.toInstant().isAfter(skewedFuture)) {
            throw new OidcAuthenticationException("Bearer token is invalid");
        }
    }

    private AuthPrincipal mapPrincipal(JWTClaimsSet claims) throws ParseException {
        String subject = claims.getSubject();
        if (StrKit.isBlank(subject)) {
            throw new OidcAuthenticationException("Bearer token is invalid");
        }
        AuthPrincipal principal = new AuthPrincipal()
                .setLoginId(subject)
                .setUserId(subject)
                .setOrgId(claims.getClaim("tenant_id"))
                .setPerms(stringSet(claims.getClaim("permissions")))
                .setRoles(rolePairs(claims.getClaim("roles")));
        principal.setProfile(Map.of("issuer", claims.getIssuer(), "subject", subject));
        return principal;
    }

    private Set<String> stringSet(Object value) {
        Set<String> result = new HashSet<>();
        if (value instanceof Collection<?>) {
            for (Object item : (Collection<?>) value) {
                if (item instanceof String && !StrKit.isBlank((String) item)) {
                    result.add((String) item);
                }
            }
        }
        return result;
    }

    private List<AuthPrincipal.RolePair> rolePairs(Object value) {
        List<AuthPrincipal.RolePair> roles = new ArrayList<>();
        for (String role : stringSet(value)) {
            roles.add(new AuthPrincipal.RolePair().setRoleId(role).setRoleCode(role).setRoleName(role));
        }
        return roles;
    }
}
