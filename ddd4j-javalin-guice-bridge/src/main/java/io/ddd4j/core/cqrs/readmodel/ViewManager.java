package io.ddd4j.core.cqrs.readmodel;

/**
 * CQRS 读侧视图管理器 SPI（纯 Java，框架无关）。
 *
 * <p>负责：
 * <ul>
 *   <li>启动：注册所有 {@link io.ddd4j.core.cqrs.query.query.DddView}，按 CRON 调度增量拉取</li>
 *   <li>停止：取消所有调度任务，关闭资源</li>
 *   <li>手动触发：支持业务侧强制立即拉取</li>
 * </ul>
 *
 * <p>本接口定义 SPI，具体实现由各框架适配层提供：
 * <ul>
 *   <li>{@code ddd4j-runtime-spring}：{@code SpringJpaViewManager}（基于 {@code SchedulingConfigurer}）</li>
 *   <li>{@code ddd4j-runtime-quarkus}：{@code QuarkusJpaViewManager}（基于 {@code @Scheduled}）</li>
 *   <li>{@code ddd4j-javalin}：{@code JavalinViewManager}（手动 / 自定义线程池）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @see ProjectionPosition
 * @see ProjectionPositionRepository
 * @since 2.0.x
 */
public interface ViewManager {

    /**
     * 启动视图管理器（注册所有 View + 启动调度）。
     */
    void start();

    /**
     * 停止视图管理器（取消所有调度任务）。
     */
    void stop();

    /**
     * 判断是否已启动。
     */
    boolean isRunning();

    /**
     * 手动触发一次全量拉取（不依赖调度）。
     *
     * <p>可用于：系统启动后立即拉取、运维触发、测试场景。
     */
    void triggerOnce();
}
