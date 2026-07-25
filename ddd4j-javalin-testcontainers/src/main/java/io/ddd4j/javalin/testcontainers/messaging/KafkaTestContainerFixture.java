package io.ddd4j.javalin.testcontainers.messaging;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Kafka container fixture (Confluent image).
 *
 * <p>Uses {@code confluentinc/cp-kafka:7.5.0} with default KRaft single-node setup.
 * Exposes bootstrap servers on the {@code PLAINTEXT://host:port} URL produced by
 * {@link KafkaContainer#getBootstrapServers()}.
 *
 * <p>Use to verify {@code ddd4j-javalin-mq-kafka} against a real broker without standing up
 * a long-running cluster.
 */
public class KafkaTestContainerFixture extends AbstractTestContainerFixture<KafkaContainer> {

    public static final String DEFAULT_IMAGE = "confluentinc/cp-kafka:7.5.0";

    @Override
    public KafkaContainer newContainer() {
        return new KafkaContainer(DockerImageName.parse(DEFAULT_IMAGE)).withReuse(true);
    }

    @Override
    protected String resolveConnectionString(KafkaContainer container) {
        return container.getBootstrapServers();
    }
}