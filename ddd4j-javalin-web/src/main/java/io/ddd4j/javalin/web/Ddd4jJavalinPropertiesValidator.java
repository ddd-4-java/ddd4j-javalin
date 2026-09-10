package io.ddd4j.javalin.web;

import io.ddd4j.kit.lang.StrKit;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/** 在创建 Guice 和监听端口前校验 Javalin 配置。 */
public final class Ddd4jJavalinPropertiesValidator {

    private Ddd4jJavalinPropertiesValidator() {
    }

    /** 校验启动所需的全部基础属性。 */
    public static void validate(Ddd4jJavalinProperties properties) {
        Ddd4jJavalinProperties value = Objects.requireNonNull(properties, "properties must not be null");
        require(StrKit.isNotBlank(value.getHost()), "host must not be blank");
        require(value.getPort() >= 0 && value.getPort() <= 65535, "port must be between 0 and 65535");
        String contextPath = value.getContextPath();
        require(StrKit.isNotBlank(contextPath) && contextPath.startsWith("/"),
                "contextPath must start with /");
        require("/".equals(contextPath) || !contextPath.endsWith("/"),
                "contextPath must not end with /");
        require(value.getMaxUploadSizeBytes() > 0, "maxUploadSizeBytes must be positive");
        require(value.getRequestTimeoutMs() > 0, "requestTimeoutMs must be positive");
        require(Objects.nonNull(value.getPublicPaths()), "publicPaths must not be null");
        require(Arrays.stream(value.getPublicPaths()).allMatch(path -> StrKit.isNotBlank(path) && path.startsWith("/")),
                "publicPaths must contain absolute paths");
        require(Objects.nonNull(value.getAllowedOrigins()), "allowedOrigins must not be null");
        require(Objects.nonNull(value.getDefaultAuthenticationMode()),
                "defaultAuthenticationMode must not be null");
        require(Objects.nonNull(value.getRuntimeMode()), "runtimeMode must not be null");
        if (value.isIdempotencyEnabled()) {
            require(Objects.nonNull(value.getIdempotencyDeploymentMode()),
                    "idempotencyDeploymentMode must not be null");
            require(StrKit.isNotBlank(value.getIdempotencyCacheName()),
                    "idempotencyCacheName must not be blank");
            Duration ttl = value.getIdempotencyTtl();
            require(Objects.nonNull(ttl) && !ttl.isZero() && !ttl.isNegative(),
                    "idempotencyTtl must be positive");
        }
        if (value.getRuntimeMode() == Ddd4jJavalinRuntimeMode.PRODUCTION && value.isCors()) {
            require(value.getAllowedOrigins().length > 0, "allowedOrigins are required in production");
            require(Arrays.stream(value.getAllowedOrigins()).noneMatch("*"::equals),
                    "allowedOrigins must not contain wildcard in production");
        }
        if (value.getRuntimeMode() == Ddd4jJavalinRuntimeMode.PRODUCTION && value.isIdempotencyEnabled()) {
            require(value.getIdempotencyDeploymentMode() == IdempotencyDeploymentMode.SHARED,
                    "SHARED idempotency is required in production");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
