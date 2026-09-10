package io.ddd4j.javalin.web;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.ddd4j.core.health.ReadinessResult;
import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.cqrs.command.CommandBus;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import io.ddd4j.web.core.idempotency.IdempotencyGuard;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Production readiness and idempotency deployment contracts. */
class Ddd4jJavalinProductionReadinessTest {

    @AfterEach
    void clearRuntimeContext() {
        BaseContext.remove(SpiKeys.COMMAND_BUS);
    }

    @Test
    void shouldReturn503WhenRequiredParticipantIsUnavailable() throws Exception {
        Ddd4jJavalinProperties properties = productionProperties();
        Javalin app = Ddd4jJavalinApplication.run(
                properties, new String[0], "", participantModule(unavailableParticipant()));
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create(
                            "http://127.0.0.1:" + app.port() + "/health/readiness")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).contains("UNAVAILABLE").doesNotContain("database-password");
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldRejectLocalIdempotencyInProduction() {
        Ddd4jJavalinProperties properties = productionProperties();
        properties.setIdempotencyEnabled(true);
        properties.setIdempotencyDeploymentMode(IdempotencyDeploymentMode.LOCAL);

        assertThatThrownBy(() -> Ddd4jJavalinApplication.run(properties, new String[0], ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SHARED");
    }

    @Test
    void shouldShareIdempotencyContentionAcrossApplicationInstances() throws Exception {
        SharedGuard guard = new SharedGuard();
        Ddd4jJavalinProperties firstProperties = sharedProductionProperties();
        Ddd4jJavalinProperties secondProperties = sharedProductionProperties();
        Javalin first = Ddd4jJavalinApplication.run(firstProperties, new String[0], "",
                new Ddd4jJavalinAutoConfiguration(firstProperties, guard));
        Javalin second = Ddd4jJavalinApplication.run(secondProperties, new String[0], "",
                new Ddd4jJavalinAutoConfiguration(secondProperties, guard));
        first.post("/orders", context -> context.result("created"));
        second.post("/orders", context -> context.result("created"));
        try {
            assertThat(post(first, "shared-key").statusCode()).isEqualTo(200);
            assertThat(post(second, "shared-key").statusCode()).isEqualTo(409);
        } finally {
            first.stop();
            second.stop();
        }
    }

    @Test
    void shouldRollbackCoreWhenSharedGuardIsMissing() {
        Ddd4jJavalinProperties properties = sharedProductionProperties();

        assertThatThrownBy(() -> Ddd4jJavalinApplication.run(properties, new String[0], ""))
                .hasMessageContaining("sharedIdempotencyGuard");
        assertThat(Contexts.get(SpiKeys.COMMAND_BUS, CommandBus.class)).isEmpty();
    }

    private Ddd4jJavalinProperties productionProperties() {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setRuntimeMode(Ddd4jJavalinRuntimeMode.PRODUCTION);
        properties.setRequestLifecycle(false);
        properties.setIdempotencyEnabled(false);
        return properties;
    }

    private Ddd4jJavalinProperties sharedProductionProperties() {
        Ddd4jJavalinProperties properties = productionProperties();
        properties.setRequestLifecycle(true);
        properties.setIdempotencyEnabled(true);
        properties.setIdempotencyDeploymentMode(IdempotencyDeploymentMode.SHARED);
        properties.setDefaultAuthenticationMode(io.ddd4j.web.core.auth.AuthenticationMode.DISABLED);
        return properties;
    }

    private HttpResponse<String> post(Javalin app, String key) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/orders"))
                        .header("Idempotency-Key", key)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(), HttpResponse.BodyHandlers.ofString());
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

    private JavalinLifecycleParticipant unavailableParticipant() {
        return new JavalinLifecycleParticipant() {
            @Override
            public int order() { return 10; }
            @Override
            public void validate() { }
            @Override
            public void start() { }
            @Override
            public ReadinessResult readiness() {
                return ReadinessResult.unavailable("postgresql", "database-password");
            }
            @Override
            public void close() { }
        };
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
}
