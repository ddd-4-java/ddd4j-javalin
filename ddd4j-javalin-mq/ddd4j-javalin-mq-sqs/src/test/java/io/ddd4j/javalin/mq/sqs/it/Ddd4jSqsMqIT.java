package io.ddd4j.javalin.mq.sqs.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.sqs.Ddd4jSqsMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.cloud.LocalStackTestContainerFixture;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import io.ddd4j.mq.sqs.SqsMQClient;
import io.ddd4j.mq.sqs.SqsProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jSqsMqGuiceModule} against LocalStack (AWS SQS emulator,
 * {@code localstack/localstack:3.4}) brought up by Testcontainers, using the AWS SDK v2
 * {@code software.amazon.awssdk:sqs} client.
 *
 * <p>Verifies a real publish → queue → consume round trip through {@link SqsMQClient}:
 * create a queue, point the ddd4j listener at its URL, publish a {@link MQEvent} and assert
 * the message comes back with the same message id (carried as an SQS message attribute).
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jSqsMqIT {

    private static final String QUEUE_NAME = "ddd4j-it-queue";
    private static final String TAG = "smoke";

    @SuppressWarnings("resource")
    private static final LocalStackContainer LOCALSTACK =
            new LocalStackTestContainerFixture().newContainer();

    @Test
    void shouldResolveCoreContractsFromGuice() {
        LOCALSTACK.start();
        try {
            SqsProperties brokerProps = sqsProperties();
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("sqs");

            Injector injector = Guice.createInjector(new Ddd4jSqsMqGuiceModule(
                    new SqsMQClient(brokerProps), brokerProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(SqsMQClient.class)).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(brokerProps);
        } finally {
            LOCALSTACK.stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        LOCALSTACK.start();
        try {
            SqsProperties brokerProps = sqsProperties();
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("sqs");
            mqProps.setPersist(false);

            String queueUrl;
            try (SqsClient sqs = brokerProps.client()) {
                queueUrl = sqs.createQueue(CreateQueueRequest.builder()
                        .queueName(QUEUE_NAME).build()).queueUrl();
            }
            assertThat(queueUrl).startsWith("http://");

            SqsMQClient client = new SqsMQClient(brokerProps);
            Injector injector = Guice.createInjector(new Ddd4jSqsMqGuiceModule(client, brokerProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            // SQS has no topic: the MQListener topic must be the queue URL.
            listener.setTopic(queueUrl);
            io.ddd4j.javalin.testcontainers.messaging.JavalinMqLifecycleTestFixture.start(
                    mqClient, List.of(listener), mqProps, new JsonMQEventSerialization(), null);

            MQEvent event = new MQEvent();
            event.setMsgId("sqs-it-" + System.nanoTime());
            event.setTopic(queueUrl);
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(20)).until(() -> bean.received.get() != null);
            MQEvent received = bean.received.get();
            assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
            assertThat(received.getTopic()).isEqualTo(queueUrl);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            io.ddd4j.javalin.testcontainers.messaging.JavalinMqLifecycleTestFixture.close();
            LOCALSTACK.stop();
        }
    }

    private static SqsProperties sqsProperties() {
        SqsProperties props = new SqsProperties();
        props.setRegion("us-east-1");
        props.setAccessKey("test");
        props.setSecretKey("test");
        props.setEndpointOverride(LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
        props.setWaitTimeSeconds(1);
        props.setPollIntervalMs(200);
        return props;
    }

    /**
     * Listener bean invoked by the ddd4j consume pipeline; records the delivered event.
     */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        @MQEventListener(topic = "queueUrl-overridden-in-test", tags = TAG, group = "it-sqs-consumer")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}
