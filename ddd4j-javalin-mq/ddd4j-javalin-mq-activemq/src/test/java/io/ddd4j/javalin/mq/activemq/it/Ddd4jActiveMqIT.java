package io.ddd4j.javalin.mq.activemq.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.activemq.Ddd4jActiveMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.javalin.testcontainers.messaging.ActiveMqTestContainerFixture;
import io.ddd4j.mq.activemq.ActiveMQClient;
import io.ddd4j.mq.activemq.ActiveMQProperties;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration test for {@link Ddd4jActiveMqGuiceModule} against a real ActiveMQ Artemis
 * instance brought up by Testcontainers. 公共骨架（Guice 装配断言 + publish/consume
 * round-trip）继承自 {@link AbstractMqIntegrationTest}，此处仅保留 Artemis 连接与凭证。
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jActiveMqIT extends AbstractMqIntegrationTest<ActiveMQProperties, ActiveMQClient> {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> ACTIVEMQ = new ActiveMqTestContainerFixture().newContainer();

    @Override
    protected String brokerName() {
        return "activemq";
    }

    @Override
    protected String topicName() {
        return "ddd4j.it.activemq";
    }

    @Override
    protected GenericContainer<?> container() {
        return ACTIVEMQ;
    }

    @Override
    protected ActiveMQProperties newProperties() {
        ActiveMQProperties props = new ActiveMQProperties();
        props.setBrokerUrl("tcp://" + ACTIVEMQ.getHost() + ":"
                + ACTIVEMQ.getMappedPort(ActiveMqTestContainerFixture.OPENWIRE_PORT));
        // Artemis 镜像默认启用 security，凭证 artemis/artemis
        props.setUsername("artemis");
        props.setPassword("artemis");
        return props;
    }

    @Override
    protected ActiveMQClient newClient(ActiveMQProperties props) {
        return new ActiveMQClient(props);
    }

    @Override
    protected Module guiceModule(ActiveMQClient client, ActiveMQProperties props) {
        return new Ddd4jActiveMqGuiceModule(client, props);
    }

    @Override
    protected Class<ActiveMQClient> clientClass() {
        return ActiveMQClient.class;
    }
}
