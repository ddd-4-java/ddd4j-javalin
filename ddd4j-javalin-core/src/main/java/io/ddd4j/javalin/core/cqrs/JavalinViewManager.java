package io.ddd4j.javalin.core.cqrs;

import io.ddd4j.guice.cqrs.GuiceViewManager;

/**
 * Javalin CQRS 读侧视图管理器（基于 {@link ScheduledExecutorService}）。
 *
 * <p>实现 ddd4j-core 的 {@link ViewManager} SPI。Javalin 是轻量级框架，
 * 不内置 CRON 调度器，故使用 JDK 自带的 {@link ScheduledExecutorService}。
 *
 * <p>由 {@code ddd4j-javalin-guice} 注册为 Guice 单例 Bean，业务方无需关心。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Deprecated
public class JavalinViewManager extends GuiceViewManager {
}
