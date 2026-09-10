package io.ddd4j.javalin.core.lifecycle;

import io.ddd4j.core.health.ReadinessResult;

/** 由 Javalin Runtime 统一排序和管理的组件生命周期。 */
public interface JavalinLifecycleParticipant extends AutoCloseable {

    /** 返回启动顺序；数值较小者先启动、后关闭。 */
    int order();

    /** 在任何组件启动前执行无副作用校验。 */
    void validate();

    /** 启动组件。 */
    void start();

    /** 返回不包含凭据和原始异常的就绪状态。 */
    ReadinessResult readiness();

    /** 关闭组件；实现必须支持重复调用。 */
    @Override
    void close();
}
