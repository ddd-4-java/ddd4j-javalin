package io.ddd4j.javalin.mq.nats.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.nats.Ddd4jNatsMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.nats.NatsMQClient;
import io.ddd4j.mq.nats.NatsProperties;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * NATS JetStream 真实容器往返契约。
 *
 * <p>启动持久化 JetStream，并在消费者初始化前显式预建 file-backed stream。</p>
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jNatsMqIT extends AbstractMqIntegrationTest<NatsProperties, NatsMQClient> {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> NATS = new GenericContainer<>(
            DockerImageName.parse("nats:2-alpine"))
            .withCommand("-js")
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
    protected void preInit(NatsMQClient client, NatsProperties props, MQProperties mqProps) throws Exception {
        try (Connection connection = Nats.connect(props.getServers())) {
            connection.jetStreamManagement().addStream(StreamConfiguration.builder()
                    .name("DDD4J_IT")
                    .subjects(topicName() + ".>")
                    .storageType(StorageType.File)
                    .build());
        }
    }

    @Override
    protected Duration consumerSettleDelay() {
        return Duration.ZERO;
    }

    @Override
    protected Duration awaitTimeout() {
        return Duration.ofSeconds(10);
    }
}
