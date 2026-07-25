package io.ddd4j.javalin.mq.kafka.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.javalin.mq.kafka.Ddd4jKafkaMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.KafkaTestContainerFixture;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.kafka.KafkaMQBrokerAdapter;
import io.ddd4j.mq.kafka.KafkaMQProperties;
import io.ddd4j.mq.serialization.JsonMQMessageSerialization;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration test for {@link Ddd4jKafkaMqGuiceModule} against a real Kafka broker brought
 * up by Testcontainers.
 *
 * <p>Verifies the publish → broker → consumer wiring end-to-end without relying on the
 * existing Mockito-based unit test. Skipped unless {@code -Pjavalin-integration-tests} is
 * active and a Docker daemon is reachable.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jKafkaMqIT {

    @SuppressWarnings("resource")
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName
            .parse(KafkaTestContainerFixture.DEFAULT_IMAGE)).withReuse(true);

    @Test
    void shouldResolveCoreContractsFromGuice() {
        KAFKA.start();
        try {
            KafkaMQProperties kafkaProps = new KafkaMQProperties();
            kafkaProps.setBootstrapServers(KAFKA.getBootstrapServers());
            Ddd4jMQProperties mqProps = new Ddd4jMQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("kafka");

            Injector injector = Guice.createInjector(new Ddd4jKafkaMqGuiceModule(kafkaProps, mqProps));
            MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
            MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
            MQEventSerialization serialization = injector.getInstance(MQEventSerialization.class);

            assertThat(publisher).isNotNull();
            assertThat(brokerAdapter).isNotNull();
            assertThat(serialization).isNotNull().isInstanceOf(JsonMQMessageSerialization.class);
            assertThat(injector.getInstance(KafkaMQBrokerAdapter.class)).isNotNull();
        } finally {
            KAFKA.stop();
        }
    }

    @Test
    void shouldPublishEventThroughBaseContextWithoutError() {
        KAFKA.start();
        try {
            AbstractDdd4jMqGuiceModule module = new Ddd4jKafkaMqGuiceModule(
                    new KafkaMQProperties() {{
                        setBootstrapServers(KAFKA.getBootstrapServers());
                    }},
                    new Ddd4jMQProperties() {{
                        setEnabled(true);
                        setBroker("kafka");
                        setPersist(false);
                    }});
            Injector injector = Guice.createInjector(module);

            // Bring up the producer channel via BaseContext (mirrors MQClient#init).
            @SuppressWarnings("unchecked")
            java.util.Map<String, Consumer<MQEvent>> publishers =
                    (java.util.Map<String, Consumer<MQEvent>>) BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            if (publishers == null) {
                publishers = new java.util.concurrent.ConcurrentHashMap<>();
                BaseContext.inject(MQEvent.MQ_EVENT_PUBLISHER, publishers);
            }
            publishers.put("kafka", injector.getInstance(MQEventPublisher.class));

            MQEvent event = new MQEvent();
            event.setMsgId("kafka-it-001");
            event.setTopic("ddd4j.it.kafka");
            event.setTag("smoke");
            event.setPayload("{\"hello\":\"kafka\"}");

            assertThatNoException().isThrownBy(() -> event.publish());
        } finally {
            KAFKA.stop();
        }
    }
}