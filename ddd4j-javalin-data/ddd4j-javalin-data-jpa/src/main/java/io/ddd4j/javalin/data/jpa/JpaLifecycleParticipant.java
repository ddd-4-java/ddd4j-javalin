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

    public JpaLifecycleParticipant(EntityManagerFactory factory, boolean ownsFactory) {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
        this.ownsFactory = ownsFactory;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public void validate() {
        if (!factory.isOpen()) {
            throw new IllegalStateException("EntityManagerFactory must be open at startup");
        }
    }

    @Override
    public synchronized void start() {
        validate();
        started = true;
    }

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
