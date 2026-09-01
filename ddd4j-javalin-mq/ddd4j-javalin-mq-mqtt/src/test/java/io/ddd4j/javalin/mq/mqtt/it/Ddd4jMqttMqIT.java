package io.ddd4j.javalin.mq.mqtt.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.mqtt.Ddd4jMqttMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.mq.mqtt.MqttMQClient;
import io.ddd4j.mq.mqtt.MqttMQProperties;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Integration test for {@link Ddd4jMqttMqGuiceModule} against a real Eclipse Mosquitto
 * broker brought up by Testcontainers. 公共骨架（Guice 装配断言 + publish/consume
 * round-trip）继承自 {@link AbstractMqIntegrationTest}。
 *
 * <p>The {@code eclipse-mosquitto:2.0} image ships a restrictive default config (listens
 * on localhost only), so the container is started with the bundled {@code mosquitto-no-auth.conf}
 * ({@code listener 1883} + {@code allow_anonymous true}) to make the broker reachable from
 * the test JVM.
 *
 * <p>Paho v3 has no user properties, so the message id travels inside the serialized
 * {@link io.ddd4j.mq.event.MQEvent} payload.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jMqttMqIT extends AbstractMqIntegrationTest<MqttMQProperties, MqttMQClient> {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(
            DockerImageName.parse("eclipse-mosquitto:2.0"))
            .withExposedPorts(1883)
            .withCommand("/usr/sbin/mosquitto", "-c", "/mosquitto-no-auth.conf");

    @Override
    protected String brokerName() {
        return "mqtt";
    }

    @Override
    protected String topicName() {
        return "ddd4j.it.mqtt";
    }

    @Override
    protected GenericContainer<?> container() {
        return MOSQUITTO;
    }

    @Override
    protected MqttMQProperties newProperties() {
        MqttMQProperties props = new MqttMQProperties();
        props.setServerUri("tcp://" + MOSQUITTO.getHost() + ":" + MOSQUITTO.getMappedPort(1883));
        return props;
    }

    @Override
    protected MqttMQClient newClient(MqttMQProperties props) {
        return new MqttMQClient(props);
    }

    @Override
    protected Module guiceModule(MqttMQClient client, MqttMQProperties props) {
        return new Ddd4jMqttMqGuiceModule(client, props);
    }

    @Override
    protected Class<MqttMQClient> clientClass() {
        return MqttMQClient.class;
    }

    @Override
    protected Duration consumerSettleDelay() {
        // Paho subscribe 为同步阻塞，无需额外等待
        return Duration.ZERO;
    }

    @Override
    protected Duration awaitTimeout() {
        return Duration.ofSeconds(10);
    }
}
