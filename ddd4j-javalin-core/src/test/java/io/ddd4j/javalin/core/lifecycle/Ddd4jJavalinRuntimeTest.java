package io.ddd4j.javalin.core.lifecycle;

import io.ddd4j.core.health.ReadinessResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ordered startup, rollback and shutdown contract for the Javalin runtime.
 */
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

    /**
     * 记录生命周期回调的测试参与者。
     * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
     * @since 7.1.x
     */
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

        /**
         * 返回组件启动顺序，数值越小越先启动。
         * @return 启动顺序值。
         */
        @Override
        public int order() {
            return order;
        }

        /**
         * 启动前校验组件状态与依赖。
         */
        @Override
        public void validate() {
            events.add("validate:" + name);
        }

        /**
         * 启动组件。
         */
        @Override
        public void start() {
            events.add("start:" + name);
            if (failStart) {
                throw new IllegalStateException("start failed: " + name);
            }
        }

        /**
         * 返回组件就绪状态。
         * @return 就绪状态。
         */
        @Override
        public ReadinessResult readiness() {
            return ReadinessResult.ready(name);
        }

        /**
         * 关闭组件并释放资源。
         */
        @Override
        public void close() {
            events.add("close:" + name);
        }
    }

    /**
     * 记录关闭动作的测试组件。
     * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
     * @since 7.1.x
     */
    private static final class RecordingCloseable implements AutoCloseable {
        private final List<String> events;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingCloseable(List<String> events) {
            this.events = events;
        }

        /**
         * 关闭组件并释放资源。
         */
        @Override
        public void close() {
            if (closeCount.getAndIncrement() == 0) {
                events.add("close:core");
            }
        }
    }
}
