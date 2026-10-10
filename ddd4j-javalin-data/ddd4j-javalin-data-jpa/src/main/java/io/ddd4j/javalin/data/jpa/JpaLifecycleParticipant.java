package io.ddd4j.javalin.data.jpa;

import io.ddd4j.core.health.ReadinessResult;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import jakarta.persistence.EntityManagerFactory;

import java.util.Objects;

/** 管理 JPA EntityManagerFactory 的就绪状态和显式所有权。 */
public final class JpaLifecycleParticipant implements JavalinLifecycleParticipant {

    private final EntityManagerFactory factory;
    private final boolean ownsFactory;
    private boolean started;
    private boolean closed;

    /**
     * 构造 JpaLifecycleParticipant 实例并初始化依赖。
     * @param factory 实体管理器工厂。
     * @param ownsFactory 是否由本参与者负责关闭工厂。
     */
    public JpaLifecycleParticipant(EntityManagerFactory factory, boolean ownsFactory) {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
        this.ownsFactory = ownsFactory;
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
        if (!factory.isOpen()) {
            throw new IllegalStateException("EntityManagerFactory must be open at startup");
        }
    }

    /**
     * 启动组件。
     */
    @Override
    public synchronized void start() {
        validate();
        started = true;
    }

    /**
     * 返回组件就绪状态。
     * @return 就绪状态。
     */
    @Override
    public synchronized ReadinessResult readiness() {
        try {
            return started && !closed && factory.isOpen()
                    ? ReadinessResult.ready("data:jpa")
                    : ReadinessResult.unavailable("data:jpa", "factory unavailable");
        } catch (RuntimeException exception) {
            return ReadinessResult.unavailable("data:jpa", "factory check failed");
        }
    }

    /**
     * 关闭实体管理器工厂并释放资源。
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        if (ownsFactory && factory.isOpen()) {
            factory.close();
        }
        closed = true;
        started = false;
    }
}
