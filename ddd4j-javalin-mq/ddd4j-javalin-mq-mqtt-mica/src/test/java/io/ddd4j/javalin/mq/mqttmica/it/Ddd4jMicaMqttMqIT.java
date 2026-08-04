package io.ddd4j.javalin.mq.mqttmica.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.mqttmica.Ddd4jMqttMicaMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.mqttmica.MicaMqttMQClient;
import io.ddd4j.mq.mqttmica.MicaMqttProperties;
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
 * Integration test for {@link Ddd4jMqttMicaMqGuiceModule} against a real Eclipse Mosquitto
 * broker brought up by Testcontainers, driven by the mica-mqtt AIO client
 * ({@code org.dromara.mica-mqtt-client}).
 *
 * <p>The {@code eclipse-mosquitto:2.0} image ships a restrictive default config (listens
 * on localhost only), so the container is started with the bundled {@code mosquitto-no-auth.conf}
 * ({@code listener 1883} + {@code allow_anonymous true}) to make the broker reachable from
 * the test JVM.
 *
 * <p>Verifies a real publish → broker → consume round trip through {@link MicaMqttMQClient}.
 * The message id travels as an MQTT5 user property (mica-mqtt writes
 * {@code ddd4jMessageId}) and is restored by the consumer.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jMicaMqttMqIT {

    private static final String TOPIC = "ddd4j.it.mica";
    private static final String TAG = "smoke";

    @SuppressWarnings("resource")
    private static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(
            DockerImageName.parse("eclipse-mosquitto:2.0"))
            .withExposedPorts(1883)
            .withCommand("/usr/sbin/mosquitto", "-c", "/mosquitto-no-auth.conf");

    @Test
    void shouldResolveCoreContractsFromGuice() {
        MOSQUITTO.start();
        try {
            MicaMqttProperties brokerProps = new MicaMqttProperties();
            brokerProps.setServerIp(MOSQUITTO.getHost());
            brokerProps.setPort(MOSQUITTO.getMappedPort(1883));
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("mqtt-mica");

            Injector injector = Guice.createInjector(new Ddd4jMqttMicaMqGuiceModule(
                    new MicaMqttMQClient(brokerProps), brokerProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(MicaMqttMQClient.class)).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(brokerProps);
        } finally {
            MOSQUITTO.stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        MOSQUITTO.start();
        try {
            MicaMqttProperties brokerProps = new MicaMqttProperties();
            brokerProps.setServerIp(MOSQUITTO.getHost());
            brokerProps.setPort(MOSQUITTO.getMappedPort(1883));
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("mqtt-mica");
            mqProps.setPersist(false);

            MicaMqttMQClient client = new MicaMqttMQClient(brokerProps);
            Injector injector = Guice.createInjector(new Ddd4jMqttMicaMqGuiceModule(client, brokerProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            mqClient.init(List.of(listener), mqProps, new JsonMQEventSerialization(), null);

            MQEvent event = new MQEvent();
            event.setMsgId("mica-it-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(10)).until(() -> bean.received.get() != null);
            MQEvent received = bean.received.get();
            assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            MOSQUITTO.stop();
        }
    }

    /**
     * Listener bean invoked by the ddd4j consume pipeline; records the delivered event.
     */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "it-mica-consumer")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}
