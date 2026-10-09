package io.ddd4j.javalin.core.lifecycle;

/** ddd4j-javalin 运行时状态。 */
public enum JavalinRuntimeState {
    /** 已创建但尚未启动。 */
    NEW,
    /** 正在校验并启动生命周期参与者。 */
    STARTING,
    /** 全部参与者启动完成，运行正常。 */
    RUNNING,
    /** 正在逆序关闭生命周期参与者。 */
    DRAINING,
    /** 已完全停止，不可再次启动。 */
    STOPPED
}
