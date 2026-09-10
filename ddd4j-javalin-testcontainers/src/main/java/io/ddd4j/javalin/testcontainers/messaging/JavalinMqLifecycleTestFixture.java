package io.ddd4j.javalin.testcontainers.messaging;

import io.ddd4j.javalin.mq.core.Ddd4jJavalinMqProperties;
import io.ddd4j.javalin.mq.core.JavalinMqLifecycleParticipant;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.event.MQEventStorer;
import io.ddd4j.mq.listener.MQListener;

import java.util.List;
import java.util.Objects;

/** 让独立 broker IT 通过 Javalin 的生产 MQ 生命周期启动和关闭。 */
public final class JavalinMqLifecycleTestFixture {

    private static final ThreadLocal<JavalinMqLifecycleParticipant> ACTIVE = new ThreadLocal<>();

    private JavalinMqLifecycleTestFixture() {
    }

    /** 启动严格 MQ 生命周期，并记录到当前测试线程。 */
    public static void start(MQClient client, List<MQListener> listeners, MQProperties properties,
                             MQEventSerialization serialization, MQEventStorer<MQEvent> storer) {
        close();
        Ddd4jJavalinMqProperties integration = new Ddd4jJavalinMqProperties();
        JavalinMqLifecycleParticipant participant = new JavalinMqLifecycleParticipant(
                Objects.requireNonNull(client, "client must not be null"),
                Objects.requireNonNull(properties, "properties must not be null"),
                integration,
                () -> List.copyOf(Objects.requireNonNull(listeners, "listeners must not be null")),
                Objects.requireNonNull(serialization, "serialization must not be null"),
                storer);
        participant.start();
        ACTIVE.set(participant);
    }

    /** 关闭当前测试线程启动的 MQ 生命周期。 */
    public static void close() {
        JavalinMqLifecycleParticipant participant = ACTIVE.get();
        if (Objects.nonNull(participant)) {
            try {
                participant.close();
            } finally {
                ACTIVE.remove();
            }
        }
    }
}
