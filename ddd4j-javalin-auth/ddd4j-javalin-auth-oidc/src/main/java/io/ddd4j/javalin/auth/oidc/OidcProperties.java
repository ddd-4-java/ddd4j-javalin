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

    /**
     * 创建配置构建器。
     * @return 配置构建器。
     */
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

        /**
         * 设置 OIDC 签发者地址。
         * @param issuer OIDC 签发者地址。
         * @return 构建器自身，支持链式调用。
         */
        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        /**
         * 设置 JWKS 地址。
         * @param jwksUri JWKS 地址。
         * @return 构建器自身，支持链式调用。
         */
        public Builder jwksUri(URI jwksUri) {
            this.jwksUri = jwksUri;
            return this;
        }

        /**
         * 设置受众（audience）集合。
         * @param audiences 受众集合。
         * @return 构建器自身，支持链式调用。
         */
        public Builder audiences(Set<String> audiences) {
            this.audiences = audiences;
            return this;
        }

        /**
         * 设置允许的签名算法集合。
         * @param allowedAlgorithms 允许的签名算法集合。
         * @return 构建器自身，支持链式调用。
         */
        public Builder allowedAlgorithms(Set<String> allowedAlgorithms) {
            this.allowedAlgorithms = allowedAlgorithms;
            return this;
        }

        /**
         * 设置允许的时钟偏移。
         * @param clockSkew 允许的时钟偏移。
         * @return 构建器自身，支持链式调用。
         */
        public Builder clockSkew(Duration clockSkew) {
            this.clockSkew = clockSkew;
            return this;
        }

        /**
         * 构建不可变的 OIDC 配置。
         * @return 构建出的不可变配置。
         */
        public OidcProperties build() {
            return new OidcProperties(this);
        }
    }
}
