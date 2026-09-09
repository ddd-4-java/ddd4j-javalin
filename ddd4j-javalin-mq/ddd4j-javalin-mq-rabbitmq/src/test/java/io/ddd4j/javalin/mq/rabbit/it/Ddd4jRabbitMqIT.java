package io.ddd4j.javalin.mq.rabbit.it;

import com.google.inject.Module;
import com.google.inject.Guice;
import io.ddd4j.javalin.mq.rabbit.Ddd4jRabbitMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.javalin.testcontainers.messaging.RabbitMqTestContainerFixture;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.MQClient;
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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jRabbitMqGuiceModule} against a real RabbitMQ broker
 * brought up by Testcontainers. 公共骨架继承自 {@link AbstractMqIntegrationTest}；
 * RabbitMQ 差异：preInit 中显式指定内置 topic exchange。
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRabbitMqIT extends AbstractMqIntegrationTest<RabbitMQProperties, RabbitMQClient> {

    @SuppressWarnings("resource")
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName
            .parse(RabbitMqTestContainerFixture.DEFAULT_IMAGE)).withReuse(true);

    @Override
    protected String brokerName() {
        return "rabbit";
    }

    @Override
    protected String topicName() {
        return "ddd4j.it.rabbit";
    }

    @Override
    protected RabbitMQContainer container() {
        return RABBIT;
    }

    @Override
    protected RabbitMQProperties newProperties() {
        RabbitMQProperties props = new RabbitMQProperties();
        props.setHost(RABBIT.getHost());
        props.setPort(RABBIT.getAmqpPort());
        props.setUsername(RABBIT.getAdminUsername());
        props.setPassword(RABBIT.getAdminPassword());
        return props;
    }

    @Override
    protected RabbitMQClient newClient(RabbitMQProperties props) {
        return new RabbitMQClient(props);
    }

    @Override
    protected Module guiceModule(RabbitMQClient client, RabbitMQProperties props) {
        return new Ddd4jRabbitMqGuiceModule(client, props);
    }

    @Override
    protected Class<RabbitMQClient> clientClass() {
        return RabbitMQClient.class;
    }

    @Override
    protected void preInit(RabbitMQClient client, RabbitMQProperties props, MQProperties mqProps) {
        // RabbitMQClient 不自动声明 exchange：显式使用内置 topic exchange，
        // 否则默认 ""（default exchange）无法 queueBind，消息会无队列可投。
        mqProps.setExchange("amq.topic");
    }

    @Test
    void shouldNackAndRedeliverWhenPersistenceFailsBeforeAcknowledgment() throws Exception {
        RABBIT.start();
        try {
            RabbitMQProperties brokerProps = newProperties();
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker(brokerName());
            mqProps.setExchange("amq.topic");
            mqProps.setPersist(true);
            RabbitMQClient client = newClient(brokerProps);
            MQClient mqClient = Guice.createInjector(guiceModule(client, brokerProps)).getInstance(MQClient.class);
            RedeliveryListener bean = new RedeliveryListener();
            Method onMessage = RedeliveryListener.class.getMethod("onMessage", MQEvent.class);
            MQListener listener = MQListener.of(bean, onMessage, onMessage.getAnnotation(MQEventListener.class));
            listener.setTopic(topicName());
            listener.setGroup("it-rabbit-redelivery-consumer");
            listener.setTags(TAG);
            AtomicInteger storeAttempts = new AtomicInteger();
            AtomicReference<String> storedMessageId = new AtomicReference<>();
            mqClient.init(Collections.singletonList(listener), mqProps, new JsonMQEventSerialization(), event -> {
                if (storeAttempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("simulated persistence outage");
                }
                storedMessageId.set(event.getMsgId());
            });

            Thread.sleep(consumerSettleDelay().toMillis());
            MQEvent event = new MQEvent();
            event.setMsgId("rabbit-redelivery-it-" + System.nanoTime());
            event.setTopic(topicName());
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(20)).until(() -> bean.received.get() != null);
            assertThat(storeAttempts.get()).isGreaterThanOrEqualTo(2);
            assertThat(storedMessageId.get()).isEqualTo(event.getMsgId());
            assertThat(bean.received.get().getMsgId()).isEqualTo(event.getMsgId());
            assertThat(bean.invocations.get()).isEqualTo(1);
        } finally {
            RABBIT.stop();
        }
    }

    /** Listener used to prove that storage failure is nacked and redelivered before business handling. */
    public static class RedeliveryListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();
        final AtomicInteger invocations = new AtomicInteger();

        @MQEventListener(topic = "", tags = TAG, group = "")
        public void onMessage(MQEvent event) {
            invocations.incrementAndGet();
            received.set(event);
        }
    }
}
