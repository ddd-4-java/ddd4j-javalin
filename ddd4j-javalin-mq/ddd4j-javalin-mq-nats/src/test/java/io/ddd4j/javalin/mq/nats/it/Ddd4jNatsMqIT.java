package io.ddd4j.javalin.mq.nats.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.nats.Ddd4jNatsMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.nats.NatsMQClient;
import io.ddd4j.mq.nats.NatsProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jNatsMqGuiceModule} against a real NATS server
 * ({@code nats:2-alpine}, core NATS without JetStream) brought up by Testcontainers.
 *
 * <p>Verifies a real publish → broker → consume round trip through {@link NatsMQClient}:
 * the adapter tries JetStream first and transparently falls back to core NATS
 * publish/subscribe when JetStream is not enabled on the server.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jNatsMqIT {

    private static final String TOPIC = "ddd4j.it.nats";
    private static final String TAG = "smoke";

    @SuppressWarnings("resource")
    private static final GenericContainer<?> NATS = new GenericContainer<>(
            DockerImageName.parse("nats:2-alpine"))
            .withExposedPorts(4222);

    @Test
    void shouldResolveCoreContractsFromGuice() {
        NATS.start();
        try {
            NatsProperties brokerProps = new NatsProperties();
            brokerProps.setServers("nats://" + NATS.getHost() + ":" + NATS.getMappedPort(4222));
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("nats");

            Injector injector = Guice.createInjector(new Ddd4jNatsMqGuiceModule(
                    new NatsMQClient(brokerProps), brokerProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(NatsMQClient.class)).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(brokerProps);
        } finally {
            NATS.stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        NATS.start();
        try {
            NatsProperties brokerProps = new NatsProperties();
            brokerProps.setServers("nats://" + NATS.getHost() + ":" + NATS.getMappedPort(4222));
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("nats");
            mqProps.setPersist(false);

            NatsMQClient client = new NatsMQClient(brokerProps);
            Injector injector = Guice.createInjector(new Ddd4jNatsMqGuiceModule(client, brokerProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            mqClient.init(List.of(listener), mqProps, new JsonMQEventSerialization(), null);

            MQEvent event = new MQEvent();
            event.setMsgId("nats-it-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(10)).until(() -> bean.received.get() != null);
            MQEvent received = bean.received.get();
            assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            NATS.stop();
        }
    }

    /**
     * Listener bean invoked by the ddd4j consume pipeline; records the delivered event.
     */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "it-nats-consumer")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}
