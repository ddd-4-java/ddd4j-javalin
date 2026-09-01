package io.ddd4j.javalin.mq.rabbit.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.rabbit.Ddd4jRabbitMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.javalin.testcontainers.messaging.RabbitMqTestContainerFixture;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.rabbitmq.RabbitMQClient;
import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for {@link Ddd4jRabbitMqGuiceModule} against a real RabbitMQ broker
 * brought up by Testcontainers. 公共骨架继承自 {@link AbstractMqIntegrationTest}；
 * RabbitMQ 差异：preInit 中显式指定内置 topic exchange。
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRabbitMqIT extends AbstractMqIntegrationTest<RabbitMQProperties, RabbitMQClient> {

    @SuppressWarnings("resource")
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName
            .parse(RabbitMqTestContainerFixture.DEFAULT_IMAGE)).withReuse(true);

    @Override
    protected String brokerName() {
        return "rabbit";
    }

    @Override
    protected String topicName() {
        return "ddd4j.it.rabbit";
    }

    @Override
    protected RabbitMQContainer container() {
        return RABBIT;
    }

    @Override
    protected RabbitMQProperties newProperties() {
        RabbitMQProperties props = new RabbitMQProperties();
        props.setHost(RABBIT.getHost());
        props.setPort(RABBIT.getAmqpPort());
        props.setUsername(RABBIT.getAdminUsername());
        props.setPassword(RABBIT.getAdminPassword());
        return props;
    }

    @Override
    protected RabbitMQClient newClient(RabbitMQProperties props) {
        return new RabbitMQClient(props);
    }

    @Override
    protected Module guiceModule(RabbitMQClient client, RabbitMQProperties props) {
        return new Ddd4jRabbitMqGuiceModule(client, props);
    }

    @Override
    protected Class<RabbitMQClient> clientClass() {
        return RabbitMQClient.class;
    }

    @Override
    protected void preInit(RabbitMQClient client, RabbitMQProperties props, MQProperties mqProps) {
        // RabbitMQClient 不自动声明 exchange：显式使用内置 topic exchange，
        // 否则默认 ""（default exchange）无法 queueBind，消息会无队列可投。
        mqProps.setExchange("amq.topic");
    }
}
