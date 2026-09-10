package io.ddd4j.javalin.web;

/** HTTP 幂等状态的部署范围。 */
public enum IdempotencyDeploymentMode {
    /** 仅适用于开发或单实例部署的进程内缓存。 */
    LOCAL,
    /** 多实例共享的原子幂等 Guard。 */
    SHARED
}
