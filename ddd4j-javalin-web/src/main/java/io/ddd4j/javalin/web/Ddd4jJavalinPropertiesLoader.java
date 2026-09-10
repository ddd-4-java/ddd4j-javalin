package io.ddd4j.javalin.web;

import io.ddd4j.web.core.auth.AuthenticationMode;
import io.ddd4j.kit.lang.StrKit;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/** 按固定优先级加载 ddd4j-javalin Web 配置。 */
public final class Ddd4jJavalinPropertiesLoader {

    private static final String PREFIX = Ddd4jJavalinProperties.PREFIX + ".";

    private static final String[] KEYS = {
            "enabled", "host", "port", "context-path", "cors", "allowed-origins",
            "max-upload-size-bytes", "request-timeout-ms", "request-lifecycle", "public-paths",
            "default-authentication-mode", "trust-forwarded-headers", "idempotency-enabled",
            "idempotency-cache-name", "idempotency-ttl-ms", "idempotency-deployment-mode",
            "health-endpoint", "runtime-mode"
    };

    private Ddd4jJavalinPropertiesLoader() {
    }

    /** 从 application.properties、环境变量、系统属性和 CLI 加载配置。 */
    public static Ddd4jJavalinProperties load(String[] args) {
        Properties fileProperties = loadApplicationProperties();
        return load(args, fileProperties, System.getenv(), System.getProperties());
    }

    static Ddd4jJavalinProperties load(String[] args, Properties fileProperties,
                                       Map<String, String> environment, Properties systemProperties) {
        Objects.requireNonNull(fileProperties, "fileProperties must not be null");
        Objects.requireNonNull(environment, "environment must not be null");
        Objects.requireNonNull(systemProperties, "systemProperties must not be null");
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        apply(properties, key -> fileProperties.getProperty(PREFIX + key));
        apply(properties, key -> environment.get(environmentName(key)));
        apply(properties, key -> systemProperties.getProperty(PREFIX + key));
        applyCli(properties, args);
        return properties;
    }

    private static Properties loadApplicationProperties() {
        Properties properties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (Objects.isNull(classLoader)) {
            classLoader = Ddd4jJavalinPropertiesLoader.class.getClassLoader();
        }
        try (InputStream input = classLoader.getResourceAsStream("application.properties")) {
            if (Objects.nonNull(input)) {
                properties.load(input);
            }
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load application.properties", exception);
        }
    }

    private static void apply(Ddd4jJavalinProperties properties, Function<String, String> source) {
        for (String key : KEYS) {
            String value = source.apply(key);
            if (Objects.nonNull(value)) {
                set(properties, key, value.trim());
            }
        }
    }

    private static void set(Ddd4jJavalinProperties properties, String key, String value) {
        switch (key) {
            case "enabled" -> properties.setEnabled(booleanValue(key, value));
            case "host" -> properties.setHost(value);
            case "port" -> properties.setPort(intValue(key, value));
            case "context-path" -> properties.setContextPath(value);
            case "cors" -> properties.setCors(booleanValue(key, value));
            case "allowed-origins" -> properties.setAllowedOrigins(listValue(value));
            case "max-upload-size-bytes" -> properties.setMaxUploadSizeBytes(longValue(key, value));
            case "request-timeout-ms" -> properties.setRequestTimeoutMs(longValue(key, value));
            case "request-lifecycle" -> properties.setRequestLifecycle(booleanValue(key, value));
            case "public-paths" -> properties.setPublicPaths(listValue(value));
            case "default-authentication-mode" -> properties.setDefaultAuthenticationMode(
                    enumValue(key, value, AuthenticationMode.class));
            case "trust-forwarded-headers" -> properties.setTrustForwardedHeaders(booleanValue(key, value));
            case "idempotency-enabled" -> properties.setIdempotencyEnabled(booleanValue(key, value));
            case "idempotency-cache-name" -> properties.setIdempotencyCacheName(value);
            case "idempotency-ttl-ms" -> properties.setIdempotencyTtl(Duration.ofMillis(longValue(key, value)));
            case "idempotency-deployment-mode" -> properties.setIdempotencyDeploymentMode(
                    enumValue(key, value, IdempotencyDeploymentMode.class));
            case "health-endpoint" -> properties.setHealthEndpoint(booleanValue(key, value));
            case "runtime-mode" -> properties.setRuntimeMode(
                    enumValue(key, value, Ddd4jJavalinRuntimeMode.class));
            default -> throw new IllegalArgumentException("Unsupported Javalin property: " + key);
        }
    }

    private static void applyCli(Ddd4jJavalinProperties properties, String[] args) {
        if (Objects.isNull(args)) {
            return;
        }
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if ("--port".equals(argument)) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("--port requires a value");
                }
                properties.setPort(intValue("port", args[++index]));
            } else if (Objects.nonNull(argument) && argument.matches("\\d{1,5}")) {
                properties.setPort(intValue("port", argument));
            }
        }
    }

    private static boolean booleanValue(String key, String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw invalid(key, value);
    }

    private static int intValue(String key, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw invalid(key, value);
        }
    }

    private static long longValue(String key, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw invalid(key, value);
        }
    }

    private static <E extends Enum<E>> E enumValue(String key, String value, Class<E> type) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid(key, value);
        }
    }

    private static String[] listValue(String value) {
        if (StrKit.isBlank(value)) {
            return new String[0];
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StrKit::isNotBlank)
                .toArray(String[]::new);
    }

    private static String environmentName(String key) {
        return (PREFIX + key).toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    private static IllegalArgumentException invalid(String key, String value) {
        return new IllegalArgumentException("Invalid " + PREFIX + key + " value: " + value);
    }
}
