package io.ddd4j.javalin.auth.oidc.it;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.nimbusds.jose.util.JSONObjectUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.ddd4j.core.health.ReadinessResult;
import io.ddd4j.javalin.auth.oidc.Ddd4jOidcJavalinModule;
import io.ddd4j.javalin.auth.oidc.NimbusOidcTokenVerifier;
import io.ddd4j.javalin.auth.oidc.OidcHttpAuthentication;
import io.ddd4j.javalin.auth.oidc.OidcProperties;
import io.ddd4j.javalin.auth.oidc.OidcSubjectProvider;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import io.ddd4j.javalin.mq.rabbit.Ddd4jRabbitMqGuiceModule;
import io.ddd4j.javalin.testcontainers.auth.KeycloakTestContainerFixture;
import io.ddd4j.javalin.testcontainers.database.PostgresTestContainerFixture;
import io.ddd4j.javalin.testcontainers.messaging.RabbitMqTestContainerFixture;
import io.ddd4j.javalin.web.Ddd4jJavalinApplication;
import io.ddd4j.javalin.web.Ddd4jJavalinAutoConfiguration;
import io.ddd4j.javalin.web.Ddd4jJavalinProperties;
import io.ddd4j.javalin.web.Ddd4jJavalinRuntimeMode;
import io.ddd4j.javalin.web.IdempotencyDeploymentMode;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.rabbitmq.RabbitMQClient;
import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import io.ddd4j.web.core.auth.AuthenticationMode;
import io.ddd4j.web.core.idempotency.IdempotencyGuard;
import io.javalin.Javalin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Phase D production startup acceptance across HTTP, OIDC, PostgreSQL, MQ and shared idempotency. */
@Tag("integration")
class ProductionStartupAcceptanceIT {

    private static final String TOPIC = "ddd4j.production.acceptance";
    private static final AtomicReference<MQEvent> RECEIVED = new AtomicReference<>();

    @Test
    void shouldStartServeAndDrainProductionComposition() throws Exception {
        JdbcParticipant database = null;
        Javalin app = null;
        try (KeycloakContainer keycloak = new KeycloakTestContainerFixture().newContainer();
             PostgreSQLContainer<?> postgres = new PostgresTestContainerFixture().newContainer();
             RabbitMQContainer rabbit = new RabbitMqTestContainerFixture().newContainer()) {
            keycloak.start();
            postgres.start();
            rabbit.start();

            String issuer = keycloak.getAuthServerUrl() + "/realms/ddd4j-test";
            String token = requestToken(issuer);
            OidcProperties oidc = OidcProperties.builder()
                    .issuer(issuer)
                    .jwksUri(URI.create(issuer + "/protocol/openid-connect/certs"))
                    .audiences(Set.of("ddd4j-test-client"))
                    .build();
            OidcSubjectProvider provider = new OidcSubjectProvider(new NimbusOidcTokenVerifier(oidc));

            RabbitMQProperties rabbitProperties = new RabbitMQProperties();
            rabbitProperties.setHost(rabbit.getHost());
            rabbitProperties.setPort(rabbit.getAmqpPort());
            rabbitProperties.setUsername(rabbit.getAdminUsername());
            rabbitProperties.setPassword(rabbit.getAdminPassword());
            rabbitProperties.setEnabled(true);
            rabbitProperties.setBroker("rabbit");
            rabbitProperties.setExchange("amq.topic");

            Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            database = new JdbcParticipant(connection);
            SharedGuard guard = new SharedGuard();
            Ddd4jJavalinProperties web = productionProperties();
            RECEIVED.set(null);

            app = Ddd4jJavalinApplication.run(web, new String[0], getClass().getPackageName(),
                    new Ddd4jJavalinAutoConfiguration(web, guard),
                    new Ddd4jOidcJavalinModule(oidc),
                    new Ddd4jRabbitMqGuiceModule(new RabbitMQClient(rabbitProperties), rabbitProperties),
                    participantModule(database));
            OidcHttpAuthentication.register(app, provider,
                    Set.of("/health", "/health/readiness", "/health/liveness"));
            app.unsafe.routes.post("/production", context -> context.result("accepted"));

            assertThat(get(app, "/health/readiness", null, null).statusCode()).isEqualTo(200);
            assertThat(post(app, token, "same-key").statusCode()).isEqualTo(200);
            assertThat(post(app, token, "same-key").statusCode()).isEqualTo(409);

            MQEvent event = new MQEvent();
            event.setMsgId("production-" + System.nanoTime());
            event.setTopic(TOPIC);
            event.publish();
            await().atMost(Duration.ofSeconds(20)).until(() -> RECEIVED.get() != null);
            assertThat(RECEIVED.get().getMsgId()).isEqualTo(event.getMsgId());
        } finally {
            if (Objects.nonNull(app)) {
                app.stop();
            }
        }
        assertThat(database).isNotNull();
        assertThat(database.closed).isTrue();
    }

    private Ddd4jJavalinProperties productionProperties() {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setRuntimeMode(Ddd4jJavalinRuntimeMode.PRODUCTION);
        properties.setDefaultAuthenticationMode(AuthenticationMode.DISABLED);
        properties.setIdempotencyDeploymentMode(IdempotencyDeploymentMode.SHARED);
        return properties;
    }

    private AbstractModule participantModule(JavalinLifecycleParticipant participant) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder.newSetBinder(binder(), JavalinLifecycleParticipant.class)
                        .addBinding().toInstance(participant);
            }
        };
    }

    private String requestToken(String issuer) throws Exception {
        String form = "grant_type=password&client_id=ddd4j-test-client&username=test-user&password=test-pass";
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(issuer + "/protocol/openid-connect/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return (String) JSONObjectUtils.parse(response.body()).get("access_token");
    }

    private HttpResponse<String> post(Javalin app, String token, String key) throws Exception {
        return get(app, "/production", "Bearer " + token, key);
    }

    private HttpResponse<String> get(Javalin app, String path, String authorization,
                                     String idempotencyKey) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + app.port() + path));
        if (Objects.nonNull(authorization)) {
            request.header("Authorization", authorization);
        }
        if (Objects.nonNull(idempotencyKey)) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return HttpClient.newHttpClient().send(
                Objects.isNull(idempotencyKey)
                        ? request.GET().build()
                        : request.POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public static final class ProductionListener {
        @MQEventListener(topic = TOPIC, group = "production-acceptance")
        public void onEvent(MQEvent event) {
            RECEIVED.set(event);
        }
    }

    private static final class SharedGuard implements IdempotencyGuard {
        private final Set<String> keys = ConcurrentHashMap.newKeySet();

        @Override
        public boolean acquire(String key, Duration ttl) {
            return keys.add(key);
        }

        @Override
        public void complete(String key) {
        }

        @Override
        public void release(String key) {
            keys.remove(key);
        }
    }

    private static final class JdbcParticipant implements JavalinLifecycleParticipant {
        private final Connection connection;
        private final AtomicBoolean closed = new AtomicBoolean();

        private JdbcParticipant(Connection connection) {
            this.connection = connection;
        }

        @Override
        public int order() { return 50; }

        @Override
        public void validate() {
            try {
                if (!connection.isValid(2)) {
                    throw new IllegalStateException("PostgreSQL connection unavailable");
                }
            } catch (Exception exception) {
                throw new IllegalStateException("PostgreSQL readiness failed", exception);
            }
        }

        @Override
        public void start() { }

        @Override
        public ReadinessResult readiness() {
            try {
                return connection.isValid(2)
                        ? ReadinessResult.ready("postgresql")
                        : ReadinessResult.unavailable("postgresql", "unavailable");
            } catch (Exception exception) {
                return ReadinessResult.unavailable("postgresql", "check failed");
            }
        }

        @Override
        public void close() {
            try {
                connection.close();
            } catch (Exception exception) {
                throw new IllegalStateException("PostgreSQL close failed", exception);
            } finally {
                closed.set(true);
            }
        }
    }
}
