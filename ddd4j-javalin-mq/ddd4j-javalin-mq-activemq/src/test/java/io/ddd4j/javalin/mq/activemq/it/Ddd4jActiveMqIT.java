package io.ddd4j.javalin.mq.activemq.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.activemq.Ddd4jActiveMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.ActiveMqTestContainerFixture;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.activemq.ActiveMQClient;
import io.ddd4j.mq.activemq.ActiveMQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jActiveMqGuiceModule} against a real ActiveMQ Classic
 * instance brought up by Testcontainers. Verifies the full publish → broker → consume
 * round trip through the ddd4j {@link MQClient} pipeline.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jActiveMqIT {

    private static final String TOPIC = "ddd4j.it.activemq";
    private static final String TAG = "smoke";

    @SuppressWarnings("resource")
    private static final GenericContainer<?> ACTIVEMQ = new ActiveMqTestContainerFixture().newContainer();

    @Test
    void shouldResolveCoreContractsFromGuice() {
        ACTIVEMQ.start();
        try {
            ActiveMQProperties brokerProps = new ActiveMQProperties();
            brokerProps.setBrokerUrl("tcp://" + ACTIVEMQ.getHost() + ":"
                    + ACTIVEMQ.getMappedPort(ActiveMqTestContainerFixture.OPENWIRE_PORT));
            // Artemis 镜像默认启用 security，凭证 artemis/artemis
            brokerProps.setUsername("artemis");
            brokerProps.setPassword("artemis");
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("activemq");

            Injector injector = Guice.createInjector(new Ddd4jActiveMqGuiceModule(
                    new ActiveMQClient(brokerProps), brokerProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(ActiveMQClient.class)).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(brokerProps);
        } finally {
            ACTIVEMQ.stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        ACTIVEMQ.start();
        try {
            ActiveMQProperties brokerProps = new ActiveMQProperties();
            brokerProps.setBrokerUrl("tcp://" + ACTIVEMQ.getHost() + ":"
                    + ACTIVEMQ.getMappedPort(ActiveMqTestContainerFixture.OPENWIRE_PORT));
            // Artemis 镜像默认启用 security，凭证 artemis/artemis
            brokerProps.setUsername("artemis");
            brokerProps.setPassword("artemis");
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("activemq");
            mqProps.setPersist(false);

            ActiveMQClient client = new ActiveMQClient(brokerProps);
            Injector injector = Guice.createInjector(new Ddd4jActiveMqGuiceModule(client, brokerProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            mqClient.init(List.of(listener), mqProps, new JsonMQEventSerialization(), null);

            // Give the consumer a moment to connect to the broker.
            Thread.sleep(3000);

            MQEvent event = new MQEvent();
            event.setMsgId("activemq-it-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(20)).until(() -> bean.received.get() != null);
            MQEvent received = bean.received.get();
            assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            ACTIVEMQ.stop();
        }
    }

    /** Listener bean invoked by the ddd4j consume pipeline; records the delivered event. */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "it-activemq-consumer")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}