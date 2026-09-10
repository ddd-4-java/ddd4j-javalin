package io.ddd4j.javalin.core.lifecycle;

import io.ddd4j.core.health.ReadinessResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ordered startup, rollback and shutdown contract for the Javalin runtime. */
class Ddd4jJavalinRuntimeTest {

    @Test
    void shouldStartAscendingAndCloseDescendingExactlyOnce() {
        List<String> events = new ArrayList<>();
        RecordingParticipant second = new RecordingParticipant(20, "second", events, false);
        RecordingParticipant first = new RecordingParticipant(10, "first", events, false);
        RecordingCloseable core = new RecordingCloseable(events);
        Ddd4jJavalinRuntime runtime = new Ddd4jJavalinRuntime(List.of(second, first), core);

        runtime.start();
        runtime.close();
        runtime.close();

        assertThat(runtime.state()).isEqualTo(JavalinRuntimeState.STOPPED);
        assertThat(events).containsExactly(
                "validate:first", "validate:second",
                "start:first", "start:second",
                "close:second", "close:first", "close:core");
        assertThat(core.closeCount.get()).isEqualTo(1);
    }

    @Test
    void shouldRollbackStartedParticipantsWhenLaterStartFails() {
        List<String> events = new ArrayList<>();
        RecordingParticipant first = new RecordingParticipant(10, "first", events, false);
        RecordingParticipant failing = new RecordingParticipant(20, "failing", events, true);
        RecordingParticipant never = new RecordingParticipant(30, "never", events, false);
        RecordingCloseable core = new RecordingCloseable(events);
        Ddd4jJavalinRuntime runtime = new Ddd4jJavalinRuntime(List.of(never, failing, first), core);

        assertThatThrownBy(runtime::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failing");

        assertThat(runtime.state()).isEqualTo(JavalinRuntimeState.STOPPED);
        assertThat(events).containsExactly(
                "validate:first", "validate:failing", "validate:never",
                "start:first", "start:failing", "close:first", "close:core");
    }

    @Test
    void shouldReportParticipantReadinessOnlyWhileRunning() {
        RecordingParticipant participant = new RecordingParticipant(10, "database", new ArrayList<>(), false);
        Ddd4jJavalinRuntime runtime = new Ddd4jJavalinRuntime(
                List.of(participant), new RecordingCloseable(new ArrayList<>()));

        assertThat(runtime.readiness().ready()).isFalse();
        runtime.start();
        assertThat(runtime.readiness().ready()).isTrue();
        assertThat(runtime.readiness().results()).extracting(ReadinessResult::name)
                .containsExactly("database");
        runtime.close();
        assertThat(runtime.readiness().ready()).isFalse();
    }

    private static final class RecordingParticipant implements JavalinLifecycleParticipant {
        private final int order;
        private final String name;
        private final List<String> events;
        private final boolean failStart;

        private RecordingParticipant(int order, String name, List<String> events, boolean failStart) {
            this.order = order;
            this.name = name;
            this.events = events;
            this.failStart = failStart;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public void validate() {
            events.add("validate:" + name);
        }

        @Override
        public void start() {
            events.add("start:" + name);
            if (failStart) {
                throw new IllegalStateException("start failed: " + name);
            }
        }

        @Override
        public ReadinessResult readiness() {
            return ReadinessResult.ready(name);
        }

        @Override
        public void close() {
            events.add("close:" + name);
        }
    }

    private static final class RecordingCloseable implements AutoCloseable {
        private final List<String> events;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingCloseable(List<String> events) {
            this.events = events;
        }

        @Override
        public void close() {
            if (closeCount.getAndIncrement() == 0) {
                events.add("close:core");
            }
        }
    }
}
