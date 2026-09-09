package io.ddd4j.javalin.auth.oidc;

import io.ddd4j.kit.lang.StrKit;
import lombok.Getter;

import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * OIDC resource-server 配置，集中约束签发者、受众、算法与 JWKS 信任边界。
 */
@Getter
public final class OidcProperties {

    private final String issuer;
    private final URI jwksUri;
    private final Set<String> audiences;
    private final Set<String> allowedAlgorithms;
    private final Duration clockSkew;

    private OidcProperties(Builder builder) {
        if (StrKit.isBlank(builder.issuer)) {
            throw new IllegalArgumentException("issuer must not be blank");
        }
        this.jwksUri = Objects.requireNonNull(builder.jwksUri, "jwksUri must not be null");
        if (Objects.isNull(builder.audiences) || builder.audiences.isEmpty()) {
            throw new IllegalArgumentException("audiences must not be empty");
        }
        requireTrustedJwksUri(jwksUri);
        this.issuer = builder.issuer;
        this.audiences = immutableCopy(builder.audiences);
        this.allowedAlgorithms = immutableCopy(builder.allowedAlgorithms);
        this.clockSkew = Objects.requireNonNull(builder.clockSkew, "clockSkew must not be null");
        if (clockSkew.isNegative()) {
            throw new IllegalArgumentException("clockSkew must not be negative");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static Set<String> immutableCopy(Set<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    private static void requireTrustedJwksUri(URI uri) {
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return;
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) || StrKit.isBlank(uri.getHost())) {
            throw new IllegalArgumentException("jwksUri must use HTTPS");
        }
        try {
            if (!InetAddress.getByName(uri.getHost()).isLoopbackAddress()) {
                throw new IllegalArgumentException("jwksUri must use HTTPS outside loopback tests");
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("jwksUri host cannot be trusted", exception);
        }
    }

    /** OIDC 配置构建器。 */
    public static final class Builder {
        private String issuer;
        private URI jwksUri;
        private Set<String> audiences = Collections.emptySet();
        private Set<String> allowedAlgorithms = Collections.singleton("RS256");
        private Duration clockSkew = Duration.ofSeconds(60);

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder jwksUri(URI jwksUri) {
            this.jwksUri = jwksUri;
            return this;
        }

        public Builder audiences(Set<String> audiences) {
            this.audiences = audiences;
            return this;
        }

        public Builder allowedAlgorithms(Set<String> allowedAlgorithms) {
            this.allowedAlgorithms = allowedAlgorithms;
            return this;
        }

        public Builder clockSkew(Duration clockSkew) {
            this.clockSkew = clockSkew;
            return this;
        }

        public OidcProperties build() {
            return new OidcProperties(this);
        }
    }
}
