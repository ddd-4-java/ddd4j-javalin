package io.ddd4j.core.cqrs.readmodel;

/**
 * CQRS 视图调度接口（框架无关）。
 *
 * <p>由各框架适配层实现具体的调度策略：
 * <ul>
 *   <li>Spring：{@code TaskScheduler.schedule(CronTask)}</li>
 *   <li>Quarkus：{@code @Scheduled(cron = "...")}</li>
 *   <li>Javalin：手动创建 ScheduledExecutorService</li>
 * </ul>
 *
 * <p>本接口让 {@link ViewManager} 可以通过 SPI 注入调度实现，而不必感知具体框架。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public interface ViewScheduler {

    /**
     * 注册一个定时任务。
     *
     * @param viewName 视图名称（用于日志 / 调试）
     * @param cron     CRON 表达式
     * @param task     要执行的任务
     * @return 任务句柄，可用于取消
     */
    ViewScheduleHandle schedule(String viewName, String cron, Runnable task);

    /**
     * 视图调度任务句柄。
     */
    interface ViewScheduleHandle {
        /**
         * 取消任务。
         */
        void cancel();

        /**
         * 判断任务是否仍处于活跃状态。
         */
        boolean isActive();
    }
}
