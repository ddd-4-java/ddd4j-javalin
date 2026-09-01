package io.ddd4j.javalin.mq.pulsar.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.pulsar.Ddd4jPulsarMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.mq.pulsar.PulsarMQClient;
import io.ddd4j.mq.pulsar.PulsarProperties;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Integration test for {@link Ddd4jPulsarMqGuiceModule} against a real Apache Pulsar
 * standalone instance brought up by Testcontainers (GenericContainer, no GA Testcontainers
 * module yet). 公共骨架继承自 {@link AbstractMqIntegrationTest}。
 *
 * <p>Pulsar 差异：事件<b>不带 tag</b> 发布 —— Pulsar 适配层在 publish 时把
 * {@code :tag} 拼到物理 topic 上，而 consumer 订阅裸 topic，带 tag 的 round trip
 * 会落到不同 topic。因此 {@link #tagName()} 返回 {@code null}、listener 订阅
 * tags 为 {@code *}。
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jPulsarMqIT extends AbstractMqIntegrationTest<PulsarProperties, PulsarMQClient> {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> PULSAR = new GenericContainer<>(
            DockerImageName.parse("apachepulsar/pulsar:3.2.0"))
            .withExposedPorts(6650, 8080)
            .withCommand("bin/pulsar", "standalone")
            // standalone 默认 JVM 大堆（-Xmx2g+direct 4g）在 Docker Desktop 上 OOMKilled：收紧内存
            .withEnv("PULSAR_MEM", "-Xms512m -Xmx512m -XX:MaxDirectMemorySize=1g")
            // 端口监听早于 namespace 初始化，publish 会报 Namespace not found：
            // 等 namespace 创建完成（standalone 无 "is up" 日志，namespace 创建即 broker 就绪）
            .waitingFor(Wait.forLogMessage(".*Created namespace public/default.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(5)));

    @Override
    protected String brokerName() {
        return "pulsar";
    }

    @Override
    protected String topicName() {
        return "ddd4j.it.pulsar";
    }

    @Override
    protected GenericContainer<?> container() {
        return PULSAR;
    }

    @Override
    protected PulsarProperties newProperties() {
        PulsarProperties props = new PulsarProperties();
        props.setServiceUrl("pulsar://" + PULSAR.getHost() + ":" + PULSAR.getMappedPort(6650));
        props.setNamespace("default");
        props.setSubscriptionName("it-sub");
        return props;
    }

    @Override
    protected PulsarMQClient newClient(PulsarProperties props) {
        return new PulsarMQClient(props);
    }

    @Override
    protected Module guiceModule(PulsarMQClient client, PulsarProperties props) {
        return new Ddd4jPulsarMqGuiceModule(client, props);
    }

    @Override
    protected Class<PulsarMQClient> clientClass() {
        return PulsarMQClient.class;
    }

    @Override
    protected String tagName() {
        // No tag: the consumer subscribes to the bare physical topic
        // (tenant/namespace/topic), the producer appends ":tag" on publish.
        return null;
    }

    @Override
    protected String listenerTags() {
        return "*";
    }

    @Override
    protected Duration consumerSettleDelay() {
        // Pulsar consumer 创建为同步（receive 前已 subscribe），无需额外等待
        return Duration.ZERO;
    }
}
