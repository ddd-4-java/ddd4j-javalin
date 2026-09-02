package io.ddd4j.web.core.auth;

import io.ddd4j.kit.lang.StrKit;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import io.ddd4j.web.core.context.WebRequestContext;

/**
 * 基于精确路径和 {@code /**} 前缀模式的访问策略。
 */
public final class PathWebAccessPolicy implements WebAccessPolicy {

    private static final String PREFIX_PATTERN = "/**";
    private final List<String> disabledPatterns;
    private final AuthenticationMode defaultMode;

    public PathWebAccessPolicy(Collection<String> disabledPatterns, AuthenticationMode defaultMode) {
        Objects.requireNonNull(disabledPatterns, "disabledPatterns must not be null");
        this.disabledPatterns = disabledPatterns.stream()
                .filter(StrKit::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        this.defaultMode = Objects.requireNonNull(defaultMode, "defaultMode must not be null");
    }

    @Override
    public AuthenticationMode authenticationMode(WebRequestContext context) {
        String path = Objects.requireNonNull(context, "context must not be null").path();
        return disabledPatterns.stream().anyMatch(pattern -> matches(path, pattern))
                ? AuthenticationMode.DISABLED : defaultMode;
    }

    private boolean matches(String path, String pattern) {
        if (pattern.endsWith(PREFIX_PATTERN)) {
            String prefix = pattern.substring(0, pattern.length() - PREFIX_PATTERN.length());
            return path.equals(prefix) || path.startsWith(prefix + '/');
        }
        return path.equals(pattern);
    }
}
