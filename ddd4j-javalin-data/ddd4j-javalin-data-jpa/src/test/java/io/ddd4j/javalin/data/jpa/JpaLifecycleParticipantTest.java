package io.ddd4j.javalin.data.jpa;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import io.ddd4j.javalin.core.Ddd4jCoreGuiceModule;
import io.ddd4j.javalin.core.lifecycle.Ddd4jJavalinRuntime;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JPA lifecycle ownership and rollback contracts. */
class JpaLifecycleParticipantTest {

    @Test
    void shouldCloseOwnedFactoryExactlyOnceOnStartupRollback() {
        RecordingFactory recording = new RecordingFactory();
        Ddd4jJavalinRuntime runtime = runtime(
                new JpaLifecycleParticipant(recording.factory(), true), failingParticipant());

        assertThatThrownBy(runtime::start).hasMessage("later participant failed");
        runtime.close();

        assertThat(recording.closes).hasValue(1);
        assertThat(recording.open).isFalse();
    }

    @Test
    void shouldLeaveExternalFactoryOpenOnStartupRollback() {
        RecordingFactory recording = new RecordingFactory();
        Ddd4jJavalinRuntime runtime = runtime(
                new JpaLifecycleParticipant(recording.factory(), false), failingParticipant());

        assertThatThrownBy(runtime::start).hasMessage("later participant failed");

        assertThat(recording.closes).hasValue(0);
        assertThat(recording.open).isTrue();
    }

    private Ddd4jJavalinRuntime runtime(JavalinLifecycleParticipant first,
                                        JavalinLifecycleParticipant second) {
        return Guice.createInjector(Ddd4jCoreGuiceModule.defaults(), new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder<JavalinLifecycleParticipant> participants = Multibinder.newSetBinder(
                        binder(), JavalinLifecycleParticipant.class);
                participants.addBinding().toInstance(first);
                participants.addBinding().toInstance(second);
            }
        }).getInstance(Ddd4jJavalinRuntime.class);
    }

    private JavalinLifecycleParticipant failingParticipant() {
        return new JavalinLifecycleParticipant() {
            @Override
            public int order() {
                return 200;
            }

            @Override
            public void validate() {
            }

            @Override
            public void start() {
                throw new IllegalStateException("later participant failed");
            }

            @Override
            public io.ddd4j.core.health.ReadinessResult readiness() {
                return io.ddd4j.core.health.ReadinessResult.ready("failure-fixture");
            }

            @Override
            public void close() {
            }
        };
    }

    private static final class RecordingFactory {
        private final AtomicBoolean open = new AtomicBoolean(true);
        private final AtomicInteger closes = new AtomicInteger();

        private EntityManagerFactory factory() {
            return (EntityManagerFactory) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{EntityManagerFactory.class},
                    (proxy, method, arguments) -> switch (method.getName()) {
                        case "isOpen" -> open.get();
                        case "close" -> {
                            closes.incrementAndGet();
                            open.set(false);
                            yield null;
                        }
                        case "toString" -> "recording-emf";
                        default -> null;
                    });
        }
    }
}
