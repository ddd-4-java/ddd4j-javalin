package io.ddd4j.javalin.mq.kafka.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.kafka.Ddd4jKafkaMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.javalin.testcontainers.messaging.KafkaTestContainerFixture;
import io.ddd4j.mq.kafka.KafkaMQClient;
import io.ddd4j.mq.kafka.KafkaMQProperties;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Integration test for {@link Ddd4jKafkaMqGuiceModule} against a real Kafka broker brought
 * up by Testcontainers. 公共骨架（Guice 装配断言 + publish/consume round-trip）继承自
 * {@link AbstractMqIntegrationTest}；Kafka 差异：container 复用、autoStartConsumers、
 * 分区分配等待与更长的 await 超时。
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jKafkaMqIT extends AbstractMqIntegrationTest<KafkaMQProperties, KafkaMQClient> {

    @SuppressWarnings("resource")
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName
            .parse(KafkaTestContainerFixture.DEFAULT_IMAGE)).withReuse(true);

    @Override
    protected String brokerName() {
        return "kafka";
    }

    @Override
    protected String topicName() {
        return "ddd4j.it.kafka";
    }

    @Override
    protected KafkaContainer container() {
        return KAFKA;
    }

    @Override
    protected KafkaMQProperties newProperties() {
        KafkaMQProperties props = new KafkaMQProperties();
        props.setBootstrapServers(KAFKA.getBootstrapServers());
        props.setAutoStartConsumers(true);
        return props;
    }

    @Override
    protected KafkaMQClient newClient(KafkaMQProperties props) {
        return new KafkaMQClient(props, null);
    }

    @Override
    protected Module guiceModule(KafkaMQClient client, KafkaMQProperties props) {
        return new Ddd4jKafkaMqGuiceModule(client, props);
    }

    @Override
    protected Class<KafkaMQClient> clientClass() {
        return KafkaMQClient.class;
    }

    @Override
    protected Duration awaitTimeout() {
        // Kafka 首轮分区分配 + offset 提交较慢
        return Duration.ofSeconds(30);
    }
}
