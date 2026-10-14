package io.ddd4j.javalin.core.lifecycle;

/**
 * ddd4j-javalin 运行时状态。
 */
public enum JavalinRuntimeState {
    /** 尚未启动。 */
    NEW,
    /** 正在启动中。 */
    STARTING,
    /** 已启动并对外提供服务。 */
    RUNNING,
    /** 正在排空在途请求与任务。 */
    DRAINING,
    /** 已完全停止。 */
    STOPPED
}
