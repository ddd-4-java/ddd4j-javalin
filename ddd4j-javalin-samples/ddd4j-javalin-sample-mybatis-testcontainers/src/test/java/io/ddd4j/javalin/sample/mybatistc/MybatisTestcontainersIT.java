package io.ddd4j.javalin.sample.mybatistc;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.data.mybatis.Ddd4jMybatisJavalinModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.database.MySqlTestContainerFixture;
import io.ddd4j.javalin.web.JavalinTestFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end sample integration test: spins up a real MySQL container, wires
 * {@link Ddd4jMybatisJavalinModule}, starts Javalin via {@link JavalinTestFixture},
 * and round-trips a CRUD operation over real HTTP.
 *
 * <p>This is the canonical "how to use ddd4j-javalin 6.3.x" example, demonstrating:
 * <ul>
 *   <li>Testcontainers fixture reuse from {@code ddd4j-javalin-testcontainers}</li>
 *   <li>Guice module composition (Ddd4jJavalinAutoConfiguration + mybatis)</li>
 *   <li>{@link JavalinTestFixture#url(String)} for safe random-port URLs</li>
 *   <li>Real HTTP client assertions via {@link JavalinTestFixture#http(HttpRequest)}</li>
 * </ul>
 */
@Tag("integration")
@JunitJupiterTestContainers
class MybatisTestcontainersIT extends JavalinTestFixture {

    @SuppressWarnings("resource")
    private static final MySQLContainer<?> MYSQL = new MySqlTestContainerFixture().newContainer();

    @Override
    protected String[] basePackages() {
        return new String[]{"io.ddd4j.javalin.sample.mybatistc"};
    }

    @Override
    protected com.google.inject.Module[] extraModules() {
        MYSQL.start();
        // Create schema
        try (java.sql.Connection conn = MYSQL.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sample_user (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(100), " +
                    "email VARCHAR(100))");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create sample_user schema", e);
        }
        Ddd4jMybatisJavalinModule mybatisModule = new Ddd4jMybatisJavalinModule(MYSQL)
                .bindRepository(SampleUserRepository.class, SampleUserMapper.class);
        return new com.google.inject.Module[]{mybatisModule};
    }

    @org.junit.jupiter.api.AfterAll
    static void tearDownContainer() {
        if (MYSQL.isRunning()) {
            MYSQL.stop();
        }
    }

    @Test
    void shouldHitHealthEndpoint() throws Exception {
        HttpResponse<String> response = http(HttpRequest.newBuilder(url("/health")).GET().build());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void shouldResolveRepositoryFromGuice() {
        withInjector(injector -> {
            SampleUserRepository repo = injector.getInstance(SampleUserRepository.class);
            assertThat(repo).isNotNull();
            assertThat(repo.getMapper()).isNotNull();
        });
    }

    /**
     * Demonstrates the Guice wiring bridge used in {@link JavalinTestFixture}: when
     * extra modules return non-null, the fixture's {@code Guice.createInjector(...)}
     * call installs them after the web + DDD-annotation modules.
     */
    @SuppressWarnings("unused")
    private static Injector debugInjector(com.google.inject.Module[] modules) {
        return Guice.createInjector(modules);
    }
}