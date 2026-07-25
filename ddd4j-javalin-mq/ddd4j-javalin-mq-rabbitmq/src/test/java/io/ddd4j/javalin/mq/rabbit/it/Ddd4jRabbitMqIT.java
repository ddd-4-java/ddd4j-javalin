package io.ddd4j.javalin.mq.rabbit.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.rabbit.Ddd4jRabbitMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.RabbitMqTestContainerFixture;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.rabbitmq.RabbitMQBrokerAdapter;
import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link Ddd4jRabbitMqGuiceModule} against a real RabbitMQ broker
 * brought up by Testcontainers.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRabbitMqIT {

    @SuppressWarnings("resource")
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName
            .parse(RabbitMqTestContainerFixture.DEFAULT_IMAGE)).withReuse(true);

    @Test
    void shouldResolveCoreContractsFromGuice() {
        RABBIT.start();
        try {
            RabbitMQProperties rabbitProps = new RabbitMQProperties();
            rabbitProps.setHost(RABBIT.getHost());
            rabbitProps.setPort(RABBIT.getAmqpPort());
            rabbitProps.setUsername(RABBIT.getAdminUsername());
            rabbitProps.setPassword(RABBIT.getAdminPassword());
            Ddd4jMQProperties mqProps = new Ddd4jMQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("rabbitmq");

            Injector injector = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(rabbitProps, mqProps));

            assertThat(injector.getInstance(MQEventPublisher.class)).isNotNull();
            assertThat(injector.getInstance(MQBrokerAdapter.class)).isNotNull();
            assertThat(injector.getInstance(RabbitMQBrokerAdapter.class)).isNotNull();
        } finally {
            RABBIT.stop();
        }
    }
}