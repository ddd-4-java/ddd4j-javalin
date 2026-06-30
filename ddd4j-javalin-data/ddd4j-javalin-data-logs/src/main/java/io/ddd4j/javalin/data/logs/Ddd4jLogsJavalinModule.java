package io.ddd4j.javalin.data.logs;

import io.ddd4j.guice.data.logs.Ddd4jLogsGuiceModule;

/**
 * ddd4j-javalin API 操作日志 Guice Module。
 *
 * <p>注册 ApiOperationLogProvider 到 Guice 容器，替代 Spring Boot 的 auto-config。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Deprecated
public class Ddd4jLogsJavalinModule extends Ddd4jLogsGuiceModule {
}
