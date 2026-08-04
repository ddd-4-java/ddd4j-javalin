package io.ddd4j.javalin.mq.pulsar.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.pulsar.Ddd4jPulsarMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.pulsar.PulsarMQClient;
import io.ddd4j.mq.pulsar.PulsarProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jPulsarMqGuiceModule} against a real Apache Pulsar
 * standalone instance brought up by Testcontainers (GenericContainer, no GA Testcontainers
 * module yet).
 *
 * <p>Verifies a real publish → broker → consume round trip through
 * {@link PulsarMQClient}: the event is published without a tag on purpose — the Pulsar
 * adapter appends {@code :tag} to the physical topic on publish while the consumer
 * subscribes to the bare topic, so a tagged round trip would target different topics.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jPulsarMqIT {

    private static final String TOPIC = "ddd4j.it.pulsar";

    @SuppressWarnings("resource")
    private static final GenericContainer<?> PULSAR = new GenericContainer<>(
            DockerImageName.parse("apachepulsar/pulsar:3.2.0"))
            .withExposedPorts(6650, 8080)
            .withCommand("bin/pulsar", "standalone")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));

    @Test
    void shouldResolveCoreContractsFromGuice() {
        PULSAR.start();
        try {
            PulsarProperties brokerProps = new PulsarProperties();
            brokerProps.setServiceUrl("pulsar://" + PULSAR.getHost() + ":" + PULSAR.getMappedPort(6650));
            brokerProps.setNamespace("default");
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("pulsar");

            Injector injector = Guice.createInjector(new Ddd4jPulsarMqGuiceModule(
                    new PulsarMQClient(brokerProps), brokerProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(PulsarMQClient.class)).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(brokerProps);
        } finally {
            PULSAR.stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        PULSAR.start();
        try {
            PulsarProperties brokerProps = new PulsarProperties();
            brokerProps.setServiceUrl("pulsar://" + PULSAR.getHost() + ":" + PULSAR.getMappedPort(6650));
            brokerProps.setNamespace("default");
            brokerProps.setSubscriptionName("it-sub");
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("pulsar");
            mqProps.setPersist(false);

            PulsarMQClient client = new PulsarMQClient(brokerProps);
            Injector injector = Guice.createInjector(new Ddd4jPulsarMqGuiceModule(client, brokerProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            mqClient.init(List.of(listener), mqProps, new JsonMQEventSerialization(), null);

            // No tag: the consumer subscribes to the bare physical topic
            // (tenant/namespace/topic), the producer appends ":tag" on publish.
            MQEvent event = new MQEvent();
            event.setMsgId("pulsar-it-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.publish();

            await().atMost(Duration.ofSeconds(20)).until(() -> bean.received.get() != null);
            MQEvent received = bean.received.get();
            assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
            assertThat(received.getTopic()).isEqualTo(TOPIC);
        } finally {
            PULSAR.stop();
        }
    }

    /**
     * Listener bean invoked by the ddd4j consume pipeline; records the delivered event.
     */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        @MQEventListener(topic = TOPIC, tags = "*", group = "it-pulsar-consumer")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}
