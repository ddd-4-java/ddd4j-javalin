package io.ddd4j.javalin.testcontainers;

import io.ddd4j.javalin.testcontainers.WireMockTestContainerFixture;
import io.ddd4j.javalin.testcontainers.database.MariaDbTestContainerFixture;
import io.ddd4j.javalin.testcontainers.database.MongoDbTestContainerFixture;
import io.ddd4j.javalin.testcontainers.database.MySqlTestContainerFixture;
import io.ddd4j.javalin.testcontainers.database.PostgresTestContainerFixture;
import io.ddd4j.javalin.testcontainers.database.RedisTestContainerFixture;
import io.ddd4j.javalin.testcontainers.messaging.ActiveMqTestContainerFixture;
import io.ddd4j.javalin.testcontainers.messaging.KafkaTestContainerFixture;
import io.ddd4j.javalin.testcontainers.messaging.RabbitMqTestContainerFixture;
import io.ddd4j.javalin.testcontainers.messaging.RocketMqTestContainerFixture;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-level contract test: every fixture must be able to construct a container without
 * throwing. Actual container start (which requires Docker) is exercised separately by the
 * integration tests in the consumer modules.
 *
 * <p>Uses Testcontainers 1.20.6's {@code container.getImage()} (String) API for image
 * name assertions.
 */
class FixtureContractTest {

    private static boolean contains(GenericContainer<?> c, String fragment) {
        String image = c.getImage().toString();
        return image != null && image.contains(fragment);
    }

    @Test
    void mysqlFixtureShouldConstructContainer() {
        MySQLContainer<?> container = new MySqlTestContainerFixture().newContainer();
        assertNotNull(container);
        assertTrue(contains(container, "mysql"));
    }

    @Test
    void postgresFixtureShouldConstructContainer() {
        PostgreSQLContainer<?> container = new PostgresTestContainerFixture().newContainer();
        assertNotNull(container);
        assertTrue(contains(container, "postgres"));
    }

    @Test
    void mariadbFixtureShouldConstructContainer() {
        MariaDBContainer<?> container = new MariaDbTestContainerFixture().newContainer();
        assertNotNull(container);
        assertTrue(contains(container, "mariadb"));
    }

    @Test
    void mongodbFixtureShouldConstructContainer() {
        MongoDBContainer container = new MongoDbTestContainerFixture().newContainer();
        assertNotNull(container);
        assertTrue(contains(container, "mongo"));
    }

    @Test
    void redisFixtureShouldConstructContainer() {
        GenericContainer<?> container = new RedisTestContainerFixture().newContainer();
        assertNotNull(container);
        assertTrue(container.getExposedPorts().contains(RedisTestContainerFixture.DEFAULT_PORT));
    }

    @Test
    void kafkaFixtureShouldConstructContainer() {
        KafkaContainer container = new KafkaTestContainerFixture().newContainer();
        assertNotNull(container);
        assertTrue(contains(container, "cp-kafka"));
    }

    @Test
    void rabbitMqFixtureShouldConstructContainer() {
        RabbitMQContainer container = new RabbitMqTestContainerFixture().newContainer();
        assertNotNull(container);
        assertTrue(contains(container, "rabbitmq"));
    }

    @Test
    void activeMqFixtureShouldConstructContainer() {
        GenericContainer<?> container = new ActiveMqTestContainerFixture().newContainer();
        assertNotNull(container);
        assertTrue(contains(container, "activemq"));
    }

    @Test
    void rocketMqFixtureShouldConstructContainer() {
        GenericContainer<?> container = new RocketMqTestContainerFixture().newContainer();
        assertNotNull(container);
        assertTrue(contains(container, "rocketmq"));
    }

    @Test
    void wireMockFixtureShouldConstructContainer() {
        GenericContainer<?> container = new WireMockTestContainerFixture().newContainer();
        assertNotNull(container);
        assertNotNull(container.getImage().toString());
    }

    @Test
    void ddd4jExtensionShouldNotThrowWhenEvaluating() {
        // Just exercise the contract: must return a non-null result without throwing,
        // regardless of whether the local DOCKER_HOST env var is set.
        Ddd4jTestContainersExtension extension = new Ddd4jTestContainersExtension();
        assertNotNull(extension.evaluateExecutionCondition(null));
    }
}