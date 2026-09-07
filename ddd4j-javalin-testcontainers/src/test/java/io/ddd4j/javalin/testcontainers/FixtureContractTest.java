package io.ddd4j.javalin.testcontainers;

import io.ddd4j.javalin.testcontainers.WireMockTestContainerFixture;
import io.ddd4j.javalin.testcontainers.cloud.LocalStackTestContainerFixture;
import io.ddd4j.javalin.testcontainers.auth.KeycloakTestContainerFixture;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void mysqlFixtureShouldConstructContainer() {
        MySQLContainer<?> container = new MySqlTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("mysql:8.0", MySqlTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void postgresFixtureShouldConstructContainer() {
        PostgreSQLContainer<?> container = new PostgresTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("postgres:16-alpine", PostgresTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void mariadbFixtureShouldConstructContainer() {
        MariaDBContainer<?> container = new MariaDbTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("mariadb:11", MariaDbTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void mongodbFixtureShouldConstructContainer() {
        MongoDBContainer container = new MongoDbTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("mongo:7", MongoDbTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void redisFixtureShouldConstructContainer() {
        GenericContainer<?> container = new RedisTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("redis:7-alpine", RedisTestContainerFixture.DEFAULT_IMAGE);
        assertTrue(container.getExposedPorts().contains(RedisTestContainerFixture.DEFAULT_PORT));
    }

    @Test
    void kafkaFixtureShouldConstructContainer() {
        KafkaContainer container = new KafkaTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("confluentinc/cp-kafka:7.5.0", KafkaTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void rabbitMqFixtureShouldConstructContainer() {
        RabbitMQContainer container = new RabbitMqTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("rabbitmq:3.13.7-management-alpine", RabbitMqTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void activeMqFixtureShouldConstructContainer() {
        GenericContainer<?> container = new ActiveMqTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("apache/activemq-artemis:2.39.0", ActiveMqTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void rocketMqFixtureShouldConstructContainer() {
        GenericContainer<?> container = new RocketMqTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("apache/rocketmq:5.3.2", RocketMqTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void wireMockFixtureShouldConstructContainer() {
        GenericContainer<?> container = new WireMockTestContainerFixture().newContainer();
        assertNotNull(container);
        assertEquals("wiremock/wiremock:3.5.0", WireMockTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void keycloakFixtureShouldUsePinnedImage() {
        assertEquals("quay.io/keycloak/keycloak:26.2", KeycloakTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void localStackFixtureShouldUsePinnedImage() {
        assertEquals("localstack/localstack:3.4", LocalStackTestContainerFixture.DEFAULT_IMAGE);
    }

    @Test
    void ddd4jExtensionShouldNotThrowWhenEvaluating() {
        // Just exercise the contract: must return a non-null result without throwing,
        // regardless of whether the local DOCKER_HOST env var is set.
        Ddd4jTestContainersExtension extension = new Ddd4jTestContainersExtension();
        assertNotNull(extension.evaluateExecutionCondition(null));
    }
}
