package io.ddd4j.javalin.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for ddd4j-javalin-web, bound from {@code ddd4j.web.javalin.*}
 * keys in {@code application.yml} / {@code application.properties} / System properties /
 * environment variables (loaded by the consumer's bootstrap mechanism).
 *
 * <p>Aligned with {@code ddd4j-boot-web-webmvc}'s {@code Ddd4jWebMvcProperties} and
 * {@code ddd4j-boot-web-webflux}'s {@code Ddd4jWebFluxProperties}.
 */
@Getter
@Setter
public class Ddd4jJavalinProperties {

    public static final String PREFIX = "ddd4j.web.javalin";

    /** Whether the Javalin web layer is enabled. Default {@code true}. */
    private boolean enabled = true;

    /** HTTP port to bind. {@code 0} lets Javalin pick a random free port (useful in tests). */
    private int port = 8080;

    /** Host to bind. Default {@code 0.0.0.0}. */
    private String host = "0.0.0.0";

    /** Context path prefix. Default {@code /}. */
    private String contextPath = "/";

    /** Whether to enable CORS for all origins (dev convenience). Default {@code false}. */
    private boolean cors = false;

    /** Maximum upload size in bytes. Default 10 MB. */
    private long maxUploadSizeBytes = 10L * 1024L * 1024L;

    /** Request timeout in milliseconds. Default 30s. */
    private long requestTimeoutMs = 30_000L;

    /** Whether to install the ddd4j unified request lifecycle (auth + OTel + idempotency). */
    private boolean requestLifecycle = true;

    /** Public paths that bypass authentication. */
    private String[] publicPaths = {"/health", "/health/readiness", "/health/liveness"};

    /** Whether to expose a default {@code /health} endpoint. */
    private boolean healthEndpoint = true;
}