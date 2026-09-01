package io.ddd4j.javalin.mq.rocket.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.rocket.Ddd4jRocketMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.javalin.testcontainers.messaging.RocketMqTestContainerFixture;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.rocketmq.RocketMQClient;
import io.ddd4j.mq.rocketmq.RocketMQProperties;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link Ddd4jRocketMqGuiceModule} against a real RocketMQ
 * (namesrv + broker) brought up by Testcontainers. 公共骨架继承自
 * {@link AbstractMqIntegrationTest}；RocketMQ 差异：preInit 阶段的 producer warm-up。
 *
 * <p>The {@code apache/rocketmq:5.3.2} image does not boot anything with its default
 * CMD (role-based entrypoint), so the fixture starts namesrv and broker inside one
 * container. The broker is forced to advertise {@code 127.0.0.1} ({@code brokerIP1}) so
 * the test JVM on the host can reach it through the port mapping; the client uses the VIP
 * channel (port-2 = 10909), hence 10911 is bound to the fixed host port 10911.
 *
 * <p>Because the push consumer defaults to {@code CONSUME_FROM_LAST_OFFSET}, a warm-up
 * message is sent with a throwaway native producer first (this also auto-creates the
 * topic), then the ddd4j consumer is registered, and finally the real event is published
 * and asserted after it comes back through the broker.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRocketMqIT extends AbstractMqIntegrationTest<RocketMQProperties, RocketMQClient> {

    // Fixture 统一负责单容器双进程启动（namesrv+broker）、固定 10911 映射与 JVM 堆收紧；
    // IT 只覆盖等待超时（QEMU/arm64 首启镜像拉取可能较慢）。
    @SuppressWarnings("resource")
    private static final GenericContainer<?> ROCKETMQ = new RocketMqTestContainerFixture().newContainer()
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));

    @Override
    protected String brokerName() {
        return "rocket";
    }

    @Override
    protected String topicName() {
        // RocketMQ topic 命名仅允许 ^[%|a-zA-Z0-9_-]+$，不允许 '.'
        return "ddd4j_it_rocket";
    }

    @Override
    protected GenericContainer<?> container() {
        return ROCKETMQ;
    }

    @Override
    protected RocketMQProperties newProperties() {
        RocketMQProperties props = new RocketMQProperties();
        props.setNameServer(ROCKETMQ.getHost() + ":"
                + ROCKETMQ.getMappedPort(RocketMqTestContainerFixture.NAMESRV_PORT));
        props.setProducerGroup("it-producer-group");
        return props;
    }

    @Override
    protected RocketMQClient newClient(RocketMQProperties props) {
        return new RocketMQClient(props);
    }

    @Override
    protected Module guiceModule(RocketMQClient client, RocketMQProperties props) {
        return new Ddd4jRocketMqGuiceModule(client, props);
    }

    @Override
    protected Class<RocketMQClient> clientClass() {
        return RocketMQClient.class;
    }

    @Override
    protected void preInit(RocketMQClient client, RocketMQProperties props, MQProperties mqProps) throws Exception {
        // Warm-up: create the topic with a throwaway native producer so the ddd4j
        // push consumer (CONSUME_FROM_LAST_OFFSET) starts pulling from the latest
        // offset afterwards instead of skipping the real event.
        DefaultMQProducer warmUp = props.newProducer();
        try {
            warmUp.start();
            await().atMost(Duration.ofSeconds(60)).ignoreExceptions()
                    .untilAsserted(() -> warmUp.send(new Message(topicName(), tagName(),
                            "warmup".getBytes(StandardCharsets.UTF_8))));
        } finally {
            warmUp.shutdown();
        }
    }
}
