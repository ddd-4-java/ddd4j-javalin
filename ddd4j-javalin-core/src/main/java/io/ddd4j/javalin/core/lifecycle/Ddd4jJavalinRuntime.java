package io.ddd4j.javalin.core.lifecycle;

import com.google.inject.Inject;
import io.ddd4j.core.health.ReadinessReport;
import io.ddd4j.core.health.ReadinessResult;
import io.ddd4j.guice.Ddd4jGuiceRuntime;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 统一管理 Javalin 集成组件与上游 Guice Runtime 的启动和关闭。 */
@Slf4j
public final class Ddd4jJavalinRuntime implements AutoCloseable {

    private final List<JavalinLifecycleParticipant> participants;
    private final AutoCloseable coreRuntime;
    private final List<JavalinLifecycleParticipant> started = new ArrayList<>();
    private JavalinRuntimeState state = JavalinRuntimeState.NEW;
    private boolean coreClosed;

    /** Guice 生产构造器。 */
    @Inject
    public Ddd4jJavalinRuntime(Set<JavalinLifecycleParticipant> participants,
                              Ddd4jGuiceRuntime coreRuntime) {
        this(participants, (AutoCloseable) coreRuntime);
    }

    Ddd4jJavalinRuntime(Collection<JavalinLifecycleParticipant> participants,
                       AutoCloseable coreRuntime) {
        Objects.requireNonNull(participants, "participants must not be null");
        this.participants = participants.stream()
                .map(participant -> Objects.requireNonNull(participant, "participant must not be null"))
                .sorted(Comparator.comparingInt(JavalinLifecycleParticipant::order))
                .toList();
        this.coreRuntime = Objects.requireNonNull(coreRuntime, "coreRuntime must not be null");
    }

    /** 校验并启动全部参与者。 */
    public synchronized void start() {
        if (state != JavalinRuntimeState.NEW) {
            throw new IllegalStateException("Javalin runtime cannot start from state " + state);
        }
        state = JavalinRuntimeState.STARTING;
        try {
            participants.forEach(JavalinLifecycleParticipant::validate);
            for (JavalinLifecycleParticipant participant : participants) {
                participant.start();
                started.add(participant);
            }
            state = JavalinRuntimeState.RUNNING;
        } catch (RuntimeException | Error exception) {
            closeStartedParticipants();
            closeCoreRuntime();
            state = JavalinRuntimeState.STOPPED;
            throw exception;
        }
    }

    /** 返回当前运行状态。 */
    public synchronized JavalinRuntimeState state() {
        return state;
    }

    /** 汇总当前参与者的安全就绪状态。 */
    public synchronized ReadinessReport readiness() {
        if (state != JavalinRuntimeState.RUNNING) {
            return new ReadinessReport(false,
                    List.of(ReadinessResult.unavailable("ddd4j-javalin-runtime", state.name().toLowerCase())));
        }
        List<ReadinessResult> results = participants.stream()
                .map(this::readiness)
                .toList();
        return new ReadinessReport(results.stream().allMatch(ReadinessResult::ready), results);
    }

    /** 逆序关闭参与者并撤销上游 Guice Runtime。 */
    @Override
    public synchronized void close() {
        if (state == JavalinRuntimeState.STOPPED) {
            return;
        }
        state = JavalinRuntimeState.DRAINING;
        closeStartedParticipants();
        closeCoreRuntime();
        state = JavalinRuntimeState.STOPPED;
    }

    private ReadinessResult readiness(JavalinLifecycleParticipant participant) {
        try {
            return Objects.requireNonNullElseGet(participant.readiness(),
                    () -> ReadinessResult.unavailable(participant.getClass().getSimpleName(), "empty result"));
        } catch (RuntimeException exception) {
            log.warn("Javalin lifecycle readiness check failed: {}", participant.getClass().getName(), exception);
            return ReadinessResult.unavailable(participant.getClass().getSimpleName(), "check failed");
        }
    }

    private void closeStartedParticipants() {
        for (int index = started.size() - 1; index >= 0; index--) {
            JavalinLifecycleParticipant participant = started.get(index);
            try {
                participant.close();
            } catch (RuntimeException exception) {
                log.warn("Javalin lifecycle participant close failed: {}",
                        participant.getClass().getName(), exception);
            }
        }
        started.clear();
    }

    private void closeCoreRuntime() {
        if (coreClosed) {
            return;
        }
        try {
            coreRuntime.close();
        } catch (Exception exception) {
            log.warn("Ddd4j Guice runtime close failed", exception);
        } finally {
            coreClosed = true;
        }
    }
}
