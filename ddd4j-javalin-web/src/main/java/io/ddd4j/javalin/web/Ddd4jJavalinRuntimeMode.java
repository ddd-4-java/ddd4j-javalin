package io.ddd4j.javalin.web;

/** Javalin 集成的运行模式。 */
public enum Ddd4jJavalinRuntimeMode {
    /** 保持历史兼容并允许单实例开发默认值。 */
    DEVELOPMENT,
    /** 启用生产配置约束。 */
    PRODUCTION
}
