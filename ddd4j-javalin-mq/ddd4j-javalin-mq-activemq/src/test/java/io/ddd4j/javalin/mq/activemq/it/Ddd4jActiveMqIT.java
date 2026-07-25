package io.ddd4j.javalin.mq.activemq.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.mq.activemq.Ddd4jActiveMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.ActiveMqTestContainerFixture;
import io.ddd4j.mq.activemq.ActiveMQBrokerAdapter;
import io.ddd4j.mq.activemq.ActiveMQProperties;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link Ddd4jActiveMqGuiceModule} against a real ActiveMQ Classic
 * instance brought up by Testcontainers.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jActiveMqIT {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> ACTIVEMQ = new ActiveMqTestContainerFixture().newContainer();

    @Test
    void shouldResolveCoreContractsFromGuice() {
        ACTIVEMQ.start();
        try {
            ActiveMQProperties props = new ActiveMQProperties();
            props.setBrokerUrl("tcp://" + ACTIVEMQ.getHost() + ":"
                    + ACTIVEMQ.getMappedPort(ActiveMqTestContainerFixture.OPENWIRE_PORT));
            Ddd4jMQProperties mqProps = new Ddd4jMQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker("activemq");

            Injector injector = Guice.createInjector(new Ddd4jActiveMqGuiceModule(props, mqProps));

            assertThat(injector.getInstance(MQEventPublisher.class)).isNotNull();
            assertThat(injector.getInstance(MQBrokerAdapter.class)).isNotNull();
            assertThat(injector.getInstance(ActiveMQBrokerAdapter.class)).isNotNull();
        } finally {
            ACTIVEMQ.stop();
        }
    }
}