package io.ddd4j.javalin.core.lifecycle;

import io.ddd4j.kit.lang.StrKit;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 向生命周期参与者提供应用扫描包。
 */
public final class Ddd4jJavalinBootstrapContext {

    private final List<String> basePackages;

    /**
     * 构造 Ddd4jJavalinBootstrapContext 实例并初始化依赖。
     * @param basePackages 基础包集合。
     */
    public Ddd4jJavalinBootstrapContext(String basePackages) {
        this(StrKit.isBlank(basePackages) ? List.of() : Arrays.asList(basePackages.split(",")));
    }

    /**
     * 构造 Ddd4jJavalinBootstrapContext 实例并初始化依赖。
     * @param basePackages 基础包集合。
     */
    public Ddd4jJavalinBootstrapContext(Collection<String> basePackages) {
        Objects.requireNonNull(basePackages, "basePackages must not be null");
        this.basePackages = basePackages.stream()
                .filter(StrKit::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 返回启动扫描的基础包列表。
     * @return 基础包名列表。
     */
    public List<String> basePackages() {
        return basePackages;
    }
}
