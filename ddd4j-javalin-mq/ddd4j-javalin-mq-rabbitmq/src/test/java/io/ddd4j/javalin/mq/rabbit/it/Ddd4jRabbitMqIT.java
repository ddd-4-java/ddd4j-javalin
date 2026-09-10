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
import java.util.concurrent.atomic.AtomicInteger;
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
            // RabbitMQClient 不自动声明 exchange：显式使用内置 topic exchange，
            // 否则默认 ""（default exchange）无法 queueBind，消息会无队列可投。
            mqProps.setExchange("amq.topic");

            RabbitMQClient client = new RabbitMQClient(brokerProps);
            Injector injector = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(client, brokerProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            io.ddd4j.javalin.testcontainers.messaging.JavalinMqLifecycleTestFixture.start(
                    mqClient, List.of(listener), mqProps, new JsonMQEventSerialization(), null);

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
            io.ddd4j.javalin.testcontainers.messaging.JavalinMqLifecycleTestFixture.close();
            RABBIT.stop();
        }
    }

    @Test
    void shouldNackAndRedeliverWhenPersistenceFailsBeforeAcknowledgment() throws Exception {
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
            mqProps.setExchange("amq.topic");
            mqProps.setPersist(true);

            RabbitMQClient client = new RabbitMQClient(brokerProps);
            MQClient mqClient = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(client, brokerProps))
                    .getInstance(MQClient.class);
            RedeliveryListener bean = new RedeliveryListener();
            Method onMessage = RedeliveryListener.class.getMethod("onMessage", MQEvent.class);
            MQListener listener = MQListener.of(bean, onMessage, onMessage.getAnnotation(MQEventListener.class));
            AtomicInteger storeAttempts = new AtomicInteger();
            AtomicReference<String> storedMessageId = new AtomicReference<>();
            io.ddd4j.javalin.testcontainers.messaging.JavalinMqLifecycleTestFixture.start(
                    mqClient, List.of(listener), mqProps, new JsonMQEventSerialization(), event -> {
                if (storeAttempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("simulated persistence outage");
                }
                storedMessageId.set(event.getMsgId());
            });

            Thread.sleep(3000);
            MQEvent event = new MQEvent();
            event.setMsgId("rabbit-redelivery-it-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(20)).until(() -> bean.received.get() != null);
            assertThat(storeAttempts.get()).isGreaterThanOrEqualTo(2);
            assertThat(storedMessageId.get()).isEqualTo(event.getMsgId());
            assertThat(bean.received.get().getMsgId()).isEqualTo(event.getMsgId());
            assertThat(bean.invocations.get()).isEqualTo(1);
        } finally {
            io.ddd4j.javalin.testcontainers.messaging.JavalinMqLifecycleTestFixture.close();
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

    /** Listener used to prove that storage failure is nacked and redelivered before business handling. */
    public static class RedeliveryListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();
        final AtomicInteger invocations = new AtomicInteger();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "it-rabbit-redelivery-consumer")
        public void onMessage(MQEvent event) {
            invocations.incrementAndGet();
            received.set(event);
        }
    }
}
