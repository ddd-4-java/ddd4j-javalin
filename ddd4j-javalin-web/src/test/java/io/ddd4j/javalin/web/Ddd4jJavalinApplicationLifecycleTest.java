package io.ddd4j.javalin.web;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.cqrs.command.CommandBus;
import io.ddd4j.core.health.ReadinessResult;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Production bootstrap integration contract for the unified runtime. */
class Ddd4jJavalinApplicationLifecycleTest {

    @AfterEach
    void clearGlobalRuntime() {
        BaseContext.remove(SpiKeys.COMMAND_BUS);
    }

    @Test
    void shouldRollbackParticipantsAndCoreBeforeReturningServer() {
        List<String> events = new ArrayList<>();
        RecordingParticipant first = new RecordingParticipant(10, "first", events, false);
        RecordingParticipant failing = new RecordingParticipant(20, "failing", events, true);
        AtomicReference<Javalin> unexpectedServer = new AtomicReference<>();

        assertThatThrownBy(() -> unexpectedServer.set(Ddd4jJavalinApplication.run(
                properties(), new String[0], "", lifecycleModule(first, failing))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failing");

        if (unexpectedServer.get() != null) {
            unexpectedServer.get().stop();
        }
        assertThat(events).containsExactly(
                "validate:first", "validate:failing",
                "start:first", "start:failing", "close:first");
        assertThat(Contexts.get(SpiKeys.COMMAND_BUS, CommandBus.class)).isEmpty();
    }

    @Test
    void shouldCloseParticipantExactlyOnceWhenServerStopsRepeatedly() {
        List<String> events = new ArrayList<>();
        RecordingParticipant participant = new RecordingParticipant(10, "owned", events, false);
        Javalin app = Ddd4jJavalinApplication.run(
                properties(), new String[0], "", lifecycleModule(participant));

        app.stop();
        app.stop();

        assertThat(events).containsExactly("validate:owned", "start:owned", "close:owned");
        assertThat(Contexts.get(SpiKeys.COMMAND_BUS, CommandBus.class)).isEmpty();
    }

    private Ddd4jJavalinProperties properties() {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setRequestLifecycle(false);
        return properties;
    }

    private AbstractModule lifecycleModule(JavalinLifecycleParticipant... participants) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder<JavalinLifecycleParticipant> binder = Multibinder.newSetBinder(
                        binder(), JavalinLifecycleParticipant.class);
                for (JavalinLifecycleParticipant participant : participants) {
                    binder.addBinding().toInstance(participant);
                }
            }
        };
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
}
