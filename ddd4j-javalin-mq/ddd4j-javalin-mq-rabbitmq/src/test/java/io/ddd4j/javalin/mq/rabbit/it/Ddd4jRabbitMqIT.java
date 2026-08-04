package io.ddd4j.javalin.mq.rabbit.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.rabbit.Ddd4jRabbitMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.RabbitMqTestContainerFixture;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.rabbitmq.RabbitMQClient;
import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jRabbitMqGuiceModule} against a real RabbitMQ broker
 * brought up by Testcontainers. Verifies the full publish → broker → consume round trip
 * through the ddd4j {@link MQClient} pipeline.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRabbitMqIT {

    private static final String TOPIC = "ddd4j.it.rabbit";
    private static final String TAG = "smoke";

    @SuppressWarnings("resource")
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName
            .parse(RabbitMqTestContainerFixture.DEFAULT_IMAGE)).withReuse(true);

    @Test
    void shouldResolveCoreContractsFromGuice() {
        RABBIT.start();
        try {
            RabbitMQProperties brokerProps = new RabbitMQProperties();
            brokerProps.setHost(RABBIT.getHost());
            brokerProps.setPort(RABBIT.getAmqpPort());
            brokerProps.setUsername(RABBIT.getAdminUsername());
            brokerProps.setPassword(RABBIT.getAdminPassword());
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("rabbit");

            Injector injector = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(
                    new RabbitMQClient(brokerProps), brokerProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(RabbitMQClient.class)).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(brokerProps);
        } finally {
            RABBIT.stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        RABBIT.start();
        try {
            RabbitMQProperties brokerProps = new RabbitMQProperties();
            brokerProps.setHost(RABBIT.getHost());
            brokerProps.setPort(RABBIT.getAmqpPort());
            brokerProps.setUsername(RABBIT.getAdminUsername());
            brokerProps.setPassword(RABBIT.getAdminPassword());
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("rabbit");
            mqProps.setPersist(false);

            RabbitMQClient client = new RabbitMQClient(brokerProps);
            Injector injector = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(client, brokerProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            mqClient.init(List.of(listener), mqProps, new JsonMQEventSerialization(), null);

            // Give the consumer a moment to bind the queue.
            Thread.sleep(3000);

            MQEvent event = new MQEvent();
            event.setMsgId("rabbit-it-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(20)).until(() -> bean.received.get() != null);
            MQEvent received = bean.received.get();
            assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            RABBIT.stop();
        }
    }

    /** Listener bean invoked by the ddd4j consume pipeline; records the delivered event. */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "it-rabbit-consumer")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}