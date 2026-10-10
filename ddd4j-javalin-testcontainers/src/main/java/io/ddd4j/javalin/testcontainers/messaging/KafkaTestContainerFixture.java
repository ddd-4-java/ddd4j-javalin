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

    /** 默认容器镜像（已钉精确版本）。镜像来源：<a href="https://testcontainers.com/modules/kafka">Testcontainers Kafka 模块</a>。 */
    public static final String DEFAULT_IMAGE = "confluentinc/cp-kafka:7.5.0";

    /**
     * 创建并配置测试容器。
     * @return 配置完成的容器实例。
     */
    @Override
    public KafkaContainer newContainer() {
        return new KafkaContainer(DockerImageName.parse(DEFAULT_IMAGE)).withReuse(true);
    }

    /**
     * 由容器解析连接字符串。
     * @param container 已启动的测试容器。
     * @return 容器连接串。
     */
    @Override
    protected String resolveConnectionString(KafkaContainer container) {
        return container.getBootstrapServers();
    }
}