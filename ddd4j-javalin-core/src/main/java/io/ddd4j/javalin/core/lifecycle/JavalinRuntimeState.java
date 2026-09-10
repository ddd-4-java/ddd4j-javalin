package io.ddd4j.javalin.core.lifecycle;

/** ddd4j-javalin 运行时状态。 */
public enum JavalinRuntimeState {
    NEW,
    STARTING,
    RUNNING,
    DRAINING,
    STOPPED
}
