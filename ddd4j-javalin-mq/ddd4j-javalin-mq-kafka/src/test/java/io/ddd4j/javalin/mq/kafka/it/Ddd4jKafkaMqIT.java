package io.ddd4j.javalin.mq.kafka.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.kafka.Ddd4jKafkaMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.KafkaTestContainerFixture;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.kafka.KafkaMQClient;
import io.ddd4j.mq.kafka.KafkaMQProperties;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jKafkaMqGuiceModule} against a real Kafka broker brought
 * up by Testcontainers. Verifies the full publish → broker → consume round trip through
 * the ddd4j {@link MQClient} pipeline (aligned with the other broker ITs).
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jKafkaMqIT {

    private static final String TOPIC = "ddd4j.it.kafka";
    private static final String TAG = "smoke";

    @SuppressWarnings("resource")
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName
            .parse(KafkaTestContainerFixture.DEFAULT_IMAGE)).withReuse(true);

    @Test
    void shouldResolveCoreContractsFromGuice() {
        KAFKA.start();
        try {
            KafkaMQProperties brokerProps = new KafkaMQProperties();
            brokerProps.setBootstrapServers(KAFKA.getBootstrapServers());
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("kafka");

            Injector injector = Guice.createInjector(new Ddd4jKafkaMqGuiceModule(
                    new KafkaMQClient(brokerProps, null), brokerProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(KafkaMQClient.class)).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(brokerProps);
        } finally {
            KAFKA.stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        KAFKA.start();
        try {
            KafkaMQProperties brokerProps = new KafkaMQProperties();
            brokerProps.setBootstrapServers(KAFKA.getBootstrapServers());
            brokerProps.setAutoStartConsumers(true);
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("kafka");
            mqProps.setPersist(false);

            KafkaMQClient client = new KafkaMQClient(brokerProps, null);
            Injector injector = Guice.createInjector(new Ddd4jKafkaMqGuiceModule(client, brokerProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            io.ddd4j.javalin.testcontainers.messaging.JavalinMqLifecycleTestFixture.start(
                    mqClient, List.of(listener), mqProps, new JsonMQEventSerialization(), null);

            // Give the consumer time to finish initial partition assignment.
            Thread.sleep(3000);

            MQEvent event = new MQEvent();
            event.setMsgId("kafka-it-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(30)).until(() -> bean.received.get() != null);
            MQEvent received = bean.received.get();
            assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            io.ddd4j.javalin.testcontainers.messaging.JavalinMqLifecycleTestFixture.close();
            KAFKA.stop();
        }
    }

    /** Listener bean invoked by the ddd4j consume pipeline; records the delivered event. */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "it-kafka-consumer")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}
