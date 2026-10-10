package io.ddd4j.javalin.core.lifecycle;

import io.ddd4j.kit.lang.StrKit;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** 向生命周期参与者提供应用扫描包。 */
public final class Ddd4jJavalinBootstrapContext {

    private final List<String> basePackages;

    /**
     * 使用逗号分隔的包名字符串创建引导上下文。
     *
     * @param basePackages 逗号分隔的应用扫描包，空白时视为空扫描包
     */
    public Ddd4jJavalinBootstrapContext(String basePackages) {
        this(StrKit.isBlank(basePackages) ? List.of() : Arrays.asList(basePackages.split(",")));
    }

    /**
     * 使用包名集合创建引导上下文。
     *
     * @param basePackages 应用扫描包集合，元素会去空白、去重；集合本身不允许为 {@code null}
     * @throws NullPointerException 当 {@code basePackages} 为 {@code null} 时抛出
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
     * 返回规范化后的应用扫描包列表。
     *
     * @return 不可变的扫描包列表，元素已去空白、去重
     */
    public List<String> basePackages() {
        return basePackages;
    }
}
