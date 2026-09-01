package io.ddd4j.javalin.mq.nats.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.nats.Ddd4jNatsMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.mq.nats.NatsMQClient;
import io.ddd4j.mq.nats.NatsProperties;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Integration test for {@link Ddd4jNatsMqGuiceModule} against a real NATS server
 * ({@code nats:2-alpine}, core NATS without JetStream) brought up by Testcontainers.
 * 公共骨架继承自 {@link AbstractMqIntegrationTest}：适配层优先尝试 JetStream，
 * 服务端未启用时透明回退到 core NATS publish/subscribe。
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jNatsMqIT extends AbstractMqIntegrationTest<NatsProperties, NatsMQClient> {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> NATS = new GenericContainer<>(
            DockerImageName.parse("nats:2-alpine"))
            .withExposedPorts(4222);

    @Override
    protected String brokerName() {
        return "nats";
    }

    @Override
    protected String topicName() {
        return "ddd4j.it.nats";
    }

    @Override
    protected GenericContainer<?> container() {
        return NATS;
    }

    @Override
    protected NatsProperties newProperties() {
        NatsProperties props = new NatsProperties();
        props.setServers("nats://" + NATS.getHost() + ":" + NATS.getMappedPort(4222));
        return props;
    }

    @Override
    protected NatsMQClient newClient(NatsProperties props) {
        return new NatsMQClient(props);
    }

    @Override
    protected Module guiceModule(NatsMQClient client, NatsProperties props) {
        return new Ddd4jNatsMqGuiceModule(client, props);
    }

    @Override
    protected Class<NatsMQClient> clientClass() {
        return NatsMQClient.class;
    }

    @Override
    protected Duration consumerSettleDelay() {
        // core NATS subscribe 同步完成，无需额外等待
        return Duration.ZERO;
    }

    @Override
    protected Duration awaitTimeout() {
        return Duration.ofSeconds(10);
    }
}
