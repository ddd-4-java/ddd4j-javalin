package io.ddd4j.javalin.data.mybatis;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.ddd4j.core.health.ReadinessResult;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import org.apache.ibatis.session.SqlSession;

import java.util.Objects;

/**
 * 在统一 Javalin 生命周期中初始化 Repository mapper 并关闭 SqlSession。
 */
public final class MybatisRepositoryLifecycleParticipant implements JavalinLifecycleParticipant {

    private final Injector injector;
    private final Ddd4jMybatisJavalinModule module;
    private boolean started;
    private boolean closed;

    /**
     * 构造 MybatisRepositoryLifecycleParticipant 实例并初始化依赖。
     * @param injector Guice 注入器。
     * @param module MyBatis 装配模块。
     */
    @Inject
    public MybatisRepositoryLifecycleParticipant(Injector injector, Ddd4jMybatisJavalinModule module) {
        this.injector = Objects.requireNonNull(injector, "injector must not be null");
        this.module = Objects.requireNonNull(module, "module must not be null");
    }

    /**
     * 返回组件启动顺序，数值越小越先启动。
     * @return 启动顺序值。
     */
    @Override
    public int order() {
        return 100;
    }

    /**
     * 启动前校验组件状态与依赖。
     */
    @Override
    public void validate() {
        // Guice 已在创建参与者时验证 Injector 与 Module 依赖。
    }

    /**
     * 启动组件。
     */
    @Override
    public synchronized void start() {
        if (started) {
            return;
        }
        module.initRepositories(injector);
        started = true;
    }

    /**
     * 返回组件就绪状态。
     * @return 就绪状态。
     */
    @Override
    public synchronized ReadinessResult readiness() {
        return started && !closed
                ? ReadinessResult.ready("data:mybatis")
                : ReadinessResult.unavailable("data:mybatis", "not initialized");
    }

    /**
     * 关闭 MyBatis 注入器并释放资源。
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        if (started) {
            injector.getInstance(SqlSession.class).close();
        }
        closed = true;
        started = false;
    }
}
