package io.ddd4j.javalin.mq.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.health.ReadinessResult;
import io.ddd4j.javalin.core.Ddd4jCoreGuiceModule;
import io.ddd4j.javalin.core.Ddd4jCoreProperties;
import io.ddd4j.javalin.core.lifecycle.Ddd4jJavalinBootstrapContext;
import io.ddd4j.javalin.core.lifecycle.Ddd4jJavalinRuntime;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.event.MQEventStorer;
import io.ddd4j.mq.listener.MQListener;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Strict MQ initialization and cleanup contract owned by ddd4j-javalin. */
class JavalinMqLifecycleParticipantTest {

    @Test
    void shouldInitializeScannedListenersThroughGuiceRuntime() {
        RecordingClient client = new RecordingClient();
        MQProperties broker = brokerProperties();
        Ddd4jJavalinMqProperties integration = new Ddd4jJavalinMqProperties();
        TestMqModule mqModule = new TestMqModule(client, broker, integration);
        Injector injector = Guice.createInjector(
                new Ddd4jCoreGuiceModule(new Ddd4jCoreProperties(),
                        new Ddd4jJavalinBootstrapContext(List.of(
                                "io.ddd4j.javalin.mq.core.fixture.valid"))),
                mqModule);

        Ddd4jJavalinRuntime runtime = injector.getInstance(Ddd4jJavalinRuntime.class);
        runtime.start();

        assertThat(client.producerInitializations.get()).isEqualTo(1);
        assertThat(client.consumerInitializations.get()).isEqualTo(2);
        assertThat(client.starts.get()).isEqualTo(1);
        runtime.close();
        assertThat(client.closes.get()).isEqualTo(1);
    }

    @Test
    void shouldRejectPersistentConsumerWithoutStorer() throws Exception {
        RecordingClient client = new RecordingClient();
        MQProperties broker = brokerProperties();
        broker.setPersist(true);
        JavalinMqLifecycleParticipant participant = participant(
                client, broker, Ddd4jJavalinMqProperties.Role.BOTH, List.of(listener()), null);

        assertThatThrownBy(participant::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MQEventStorer");
    }

    @Test
    void shouldRejectRequiredConsumerWithNoListeners() {
        JavalinMqLifecycleParticipant participant = participant(
                new RecordingClient(), brokerProperties(),
                Ddd4jJavalinMqProperties.Role.CONSUMER_ONLY, List.of(), event -> {
                });

        assertThatThrownBy(participant::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("listener");
    }

    @Test
    void shouldAllowProducerOnlyWithoutListeners() {
        RecordingClient client = new RecordingClient();
        JavalinMqLifecycleParticipant participant = participant(
                client, brokerProperties(), Ddd4jJavalinMqProperties.Role.PRODUCER_ONLY,
                List.of(), null);

        assertThatCode(participant::validate).doesNotThrowAnyException();
        participant.start();
        assertThat(client.producerInitializations.get()).isEqualTo(1);
        assertThat(client.consumerInitializations.get()).isZero();
        assertThat(client.starts.get()).isEqualTo(1);
        assertThat(participant.readiness()).isEqualTo(ReadinessResult.ready("mq:recording"));
        participant.close();
        participant.close();
        assertThat(client.closes.get()).isEqualTo(1);
    }

    @Test
    void shouldPropagateConsumerInitializationFailureAndClosePartialClient() throws Exception {
        RecordingClient client = new RecordingClient();
        client.failConsumer = true;
        JavalinMqLifecycleParticipant participant = participant(
                client, brokerProperties(), Ddd4jJavalinMqProperties.Role.BOTH,
                List.of(listener()), event -> {
                });
        assertThatThrownBy(participant::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("consumer unavailable");
        assertThat(client.closes.get()).isEqualTo(1);
        assertThat(client.starts.get()).isZero();
    }

    private JavalinMqLifecycleParticipant participant(
            RecordingClient client, MQProperties broker, Ddd4jJavalinMqProperties.Role role,
            List<MQListener> listeners, MQEventStorer<MQEvent> storer) {
        Ddd4jJavalinMqProperties integration = new Ddd4jJavalinMqProperties();
        integration.setRole(role);
        integration.setRequireListeners(true);
        return new JavalinMqLifecycleParticipant(
                client, broker, integration, () -> listeners, new RecordingSerialization(), storer);
    }

    private MQProperties brokerProperties() {
        MQProperties properties = new MQProperties();
        properties.setEnabled(true);
        properties.setBroker("recording");
        return properties;
    }

    private MQListener listener() throws Exception {
        ListenerBean bean = new ListenerBean();
        Method method = ListenerBean.class.getMethod("onEvent", MQEvent.class);
        return MQListener.of(bean, method, method.getAnnotation(MQEventListener.class));
    }

    public static final class ListenerBean {
        @MQEventListener(topic = "orders", group = "production")
        public void onEvent(MQEvent event) {
        }
    }

    /**
     * Fix A：{@link JavalinMqLifecycleParticipant#close()} 应清理
     * {@link MqContextPropagator#SNAPSHOT_REGISTRY_KEY}，避免长时间运行的
     * 进程累积快照。
     */
    @Test
    void shouldClearSnapshotRegistryOnClose() {
        // 直接 seed BaseContext 模拟"在生命周期内累积了快照"
        java.util.Map<String, Object> registry = new java.util.concurrent.ConcurrentHashMap<>();
        registry.put("msg-leak", new java.util.HashMap<>());
        BaseContext.inject(MqContextPropagator.SNAPSHOT_REGISTRY_KEY, registry);
        assertThat((Object) BaseContext.get(MqContextPropagator.SNAPSHOT_REGISTRY_KEY)).isNotNull();

        JavalinMqLifecycleParticipant participant = participant(
                new RecordingClient(), brokerProperties(),
                Ddd4jJavalinMqProperties.Role.PRODUCER_ONLY, List.of(), event -> {
                });
        participant.start();
        participant.close();

        assertThat((Object) BaseContext.get(MqContextPropagator.SNAPSHOT_REGISTRY_KEY)).isNull();
    }

    private static final class RecordingClient implements MQClient {
        private final AtomicInteger producerInitializations = new AtomicInteger();
        private final AtomicInteger consumerInitializations = new AtomicInteger();
        private final AtomicInteger starts = new AtomicInteger();
        private final AtomicInteger closes = new AtomicInteger();
        private boolean failConsumer;

        @Override
        public String impl() {
            return "recording";
        }

        @Override
        public Consumer<MQEvent> initProducer(MQProperties properties) {
            producerInitializations.incrementAndGet();
            return event -> {
            };
        }

        @Override
        public boolean initConsumer(MQListener listener, MQProperties properties) {
            consumerInitializations.incrementAndGet();
            if (failConsumer) {
                throw new IllegalStateException("consumer unavailable");
            }
            return true;
        }

        @Override
        public void start() {
            starts.incrementAndGet();
        }

        @Override
        public void close() {
            closes.incrementAndGet();
        }
    }

    private static final class RecordingSerialization implements MQEventSerialization {
        @Override
        public <S, T> T deserialize(S source, Class<T> type) {
            return type.cast(source);
        }

        @Override
        public <T> T serialize(Object source) {
            @SuppressWarnings("unchecked")
            T value = (T) source;
            return value;
        }
    }

    private static final class TestMqModule extends AbstractDdd4jMqGuiceModule {
        private final RecordingClient client;

        private TestMqModule(RecordingClient client, MQProperties broker,
                             Ddd4jJavalinMqProperties integration) {
            super(broker, integration);
            this.client = client;
        }

        @Override
        protected void configure() {
            super.configure();
            bind(MQClient.class).toInstance(client);
        }

        public MQClient mqClient() {
            return client;
        }
    }
}
