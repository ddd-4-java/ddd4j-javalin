package io.ddd4j.javalin.data.mybatis;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.ddd4j.core.health.ReadinessResult;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import org.apache.ibatis.session.SqlSession;

import java.util.Objects;

/** 在统一 Javalin 生命周期中初始化 Repository mapper 并关闭 SqlSession。 */
public final class MybatisRepositoryLifecycleParticipant implements JavalinLifecycleParticipant {

    private final Injector injector;
    private final Ddd4jMybatisJavalinModule module;
    private boolean started;
    private boolean closed;

    @Inject
    public MybatisRepositoryLifecycleParticipant(Injector injector, Ddd4jMybatisJavalinModule module) {
        this.injector = Objects.requireNonNull(injector, "injector must not be null");
        this.module = Objects.requireNonNull(module, "module must not be null");
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public void validate() {
        // Guice 已在创建参与者时验证 Injector 与 Module 依赖。
    }

    @Override
    public synchronized void start() {
        if (started) {
            return;
        }
        module.initRepositories(injector);
        started = true;
    }

    @Override
    public synchronized ReadinessResult readiness() {
        return started && !closed
                ? ReadinessResult.ready("data:mybatis")
                : ReadinessResult.unavailable("data:mybatis", "not initialized");
    }

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
