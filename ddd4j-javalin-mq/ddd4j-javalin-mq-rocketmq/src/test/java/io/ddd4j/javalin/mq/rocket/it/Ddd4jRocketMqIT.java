package io.ddd4j.javalin.mq.rocket.it;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.rocket.Ddd4jRocketMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.RocketMqTestContainerFixture;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.rocketmq.RocketMQClient;
import io.ddd4j.mq.rocketmq.RocketMQProperties;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jRocketMqGuiceModule} against a real RocketMQ
 * (namesrv + broker) brought up by Testcontainers.
 *
 * <p>The {@code apache/rocketmq:5.1.0} image does not boot anything with its default
 * CMD (role-based entrypoint with {@code CMD ["dummy"]}), so the container command is
 * overridden to start namesrv and broker inside one container. The broker is forced to
 * advertise {@code 127.0.0.1} ({@code brokerIP1}) so the test JVM on the host can reach
 * it through the port mapping; the RocketMQ client uses the VIP channel (port-2 = 10909),
 * hence 10909 is bound to the fixed host port 10909.
 *
 * <p>Because the push consumer defaults to {@code CONSUME_FROM_LAST_OFFSET}, a warm-up
 * message is sent with a throwaway native producer first (this also auto-creates the
 * topic), then the ddd4j consumer is registered, and finally the real event is published
 * and asserted after it comes back through the broker.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRocketMqIT {

    private static final String TOPIC = "ddd4j.it.rocket";
    private static final String TAG = "smoke";
    /**
     * Fixed host port for the broker VIP channel (10911 - 2) so the client can reach the
     * broker advertised as {@code 127.0.0.1}.
     */
    private static final int BROKER_VIP_HOST_PORT = RocketMqTestContainerFixture.BROKER_VIP_PORT;

    @SuppressWarnings("resource")
    private static final GenericContainer<?> ROCKETMQ = new RocketMqTestContainerFixture().newContainer()
            // boot namesrv + broker in one container; advertise 127.0.0.1 so the host JVM can connect
            .withCommand("sh", "-c",
                    "echo 'brokerIP1=127.0.0.1' > /tmp/broker-it.conf; "
                            + "sh mqnamesrv & "
                            + "sleep 8; "
                            + "sh mqbroker -n localhost:9876 -c /tmp/broker-it.conf & "
                            + "wait")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
            // keep 9876 on a randomized host port, pin 10909 (VIP channel) to the same host port
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPortBindings(
                    new PortBinding(Ports.Binding.empty(),
                            new ExposedPort(RocketMqTestContainerFixture.NAMESRV_PORT)),
                    new PortBinding(Ports.Binding.bindPort(BROKER_VIP_HOST_PORT),
                            new ExposedPort(RocketMqTestContainerFixture.BROKER_VIP_PORT))));

    @Test
    void shouldResolveCoreContractsFromGuice() {
        ROCKETMQ.start();
        try {
            RocketMQProperties brokerProps = new RocketMQProperties();
            brokerProps.setNameServer(ROCKETMQ.getHost() + ":"
                    + ROCKETMQ.getMappedPort(RocketMqTestContainerFixture.NAMESRV_PORT));
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("rocket");

            Injector injector = Guice.createInjector(new Ddd4jRocketMqGuiceModule(
                    new RocketMQClient(brokerProps), brokerProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(RocketMQClient.class)).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(brokerProps);
        } finally {
            ROCKETMQ.stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        ROCKETMQ.start();
        try {
            RocketMQProperties brokerProps = new RocketMQProperties();
            brokerProps.setNameServer(ROCKETMQ.getHost() + ":"
                    + ROCKETMQ.getMappedPort(RocketMqTestContainerFixture.NAMESRV_PORT));
            brokerProps.setProducerGroup("it-producer-group");
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("rocket");
            mqProps.setPersist(false);

            // 1) Warm-up: create the topic with a throwaway native producer so the ddd4j
            //    push consumer (CONSUME_FROM_LAST_OFFSET) starts pulling from the latest
            //    offset afterwards instead of skipping the real event.
            DefaultMQProducer warmUp = brokerProps.newProducer();
            warmUp.start();
            try {
                await().atMost(Duration.ofSeconds(60)).ignoreExceptions()
                        .untilAsserted(() -> warmUp.send(new Message(TOPIC, TAG,
                                "warmup".getBytes(StandardCharsets.UTF_8))));
            } finally {
                warmUp.shutdown();
            }

            // 2) Wire the ddd4j client through Guice and register producer + consumer.
            RocketMQClient client = new RocketMQClient(brokerProps);
            Injector injector = Guice.createInjector(new Ddd4jRocketMqGuiceModule(client, brokerProps));
            MQClient mqClient = injector.getInstance(MQClient.class);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            mqClient.init(List.of(listener), mqProps, new JsonMQEventSerialization(), null);

            // Give the consumer a moment to finish the initial rebalance / pull setup.
            Thread.sleep(3000);

            // 3) Publish the real event through the ddd4j publisher and assert the round trip.
            MQEvent event = new MQEvent();
            event.setMsgId("rocket-it-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.setTag(TAG);
            event.publish();

            await().atMost(Duration.ofSeconds(20)).until(() -> bean.received.get() != null);
            MQEvent received = bean.received.get();
            assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
            assertThat(received.getTopic()).isEqualTo(TOPIC);
            assertThat(received.getTag()).isEqualTo(TAG);
        } finally {
            ROCKETMQ.stop();
        }
    }

    /**
     * Listener bean invoked by the ddd4j consume pipeline; records the delivered event.
     */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        @MQEventListener(topic = TOPIC, tags = TAG, group = "it-rocket-consumer")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}
