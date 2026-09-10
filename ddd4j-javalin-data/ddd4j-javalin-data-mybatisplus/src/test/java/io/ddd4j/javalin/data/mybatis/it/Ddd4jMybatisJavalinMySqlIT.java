package io.ddd4j.javalin.data.mybatis.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.core.Ddd4jCoreGuiceModule;
import io.ddd4j.javalin.core.lifecycle.Ddd4jJavalinRuntime;
import io.ddd4j.javalin.data.mybatis.Ddd4jMybatisJavalinModule;
import io.ddd4j.javalin.data.mybatis.TestUserMapper;
import io.ddd4j.javalin.data.mybatis.TestUserRepository;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.database.MySqlTestContainerFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@link Ddd4jMybatisJavalinModule} and the real ddd4j
 * {@code BaseRepositoryImpl} against a real MySQL 8 instance
 * brought up by Testcontainers. The complementary unit-level test
 * {@code Ddd4jMybatisJavalinModuleTest} uses an H2 in-memory database for fast feedback.
 *
 * <p>This test exercises the full CRUD + pagination stack:
 * <ul>
 *   <li>Insert aggregate root and verify auto-incremented id is returned</li>
 *   <li>Read by id, list with filter, paginated query</li>
 *   <li>Update and verify changes are visible</li>
 *   <li>Delete and verify removal</li>
 * </ul>
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jMybatisJavalinMySqlIT {

    @SuppressWarnings("resource")
    private static final MySQLContainer<?> MYSQL = new MySqlTestContainerFixture().newContainer();

    private static DataSource dataSource;
    private static Injector injector;
    private static Ddd4jJavalinRuntime runtime;

    @BeforeAll
    static void setUp() throws Exception {
        MYSQL.start();
        // Build a minimal DataSource that delegates to the MySQL driver's
        // DriverManager using the Testcontainers-provided JDBC URL. Avoids the
        // HikariCP dependency in this test scope.
        dataSource = new SimpleDriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());

        // Create the test table.
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS test_user (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(100), " +
                    "email VARCHAR(100))");
        }

        Ddd4jMybatisJavalinModule module = new Ddd4jMybatisJavalinModule(dataSource)
                .bindRepository(TestUserRepository.class, TestUserMapper.class);
        injector = Guice.createInjector(Ddd4jCoreGuiceModule.defaults(), module);
        runtime = injector.getInstance(Ddd4jJavalinRuntime.class);
        runtime.start();
    }

    @AfterAll
    static void tearDown() {
        if (runtime != null) {
            runtime.close();
        }
        MYSQL.stop();
    }

    @Test
    void shouldResolveCoreContracts() {
        assertNotNull(injector.getInstance(org.apache.ibatis.session.SqlSession.class), "SqlSession must be available");
    }

    @Test
    void shouldExecuteFullCrudFlowAgainstMysql() {
        TestUserRepository repo = injector.getInstance(TestUserRepository.class);
        assertNotNull(repo);

        // CREATE
        TestUserRepository.TestUserModel user = new TestUserRepository.TestUserModel();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        TestUserRepository.TestUserModel saved = repo.save(user);
        assertNotNull(saved);
        assertNotNull(user.getId());

        // READ
        TestUserRepository.TestUserModel found = repo.findById(user.getId()).orElseThrow();
        assertEquals("alice", found.getUsername());
        assertEquals("alice@example.com", found.getEmail());

        // UPDATE
        found.setEmail("alice@updated.com");
        TestUserRepository.TestUserModel updated = repo.updateById(found);
        assertNotNull(updated);
        TestUserRepository.TestUserModel reloaded = repo.findById(found.getId()).orElseThrow();
        assertEquals("alice@updated.com", reloaded.getEmail());

        // LIST
        TestUserRepository.TestUserQuery query = new TestUserRepository.TestUserQuery();
        List<TestUserRepository.TestUserModel> list = repo.findList(query);
        assertFalse(list.isEmpty());

        // PAGE
        io.ddd4j.core.api.Page<TestUserRepository.TestUserModel> page = repo.page(query);
        assertNotNull(page);
        assertTrue(page.getTotal() >= 1);

        // DELETE
        repo.deleteById(user.getId());
        assertTrue(repo.findById(user.getId()).isEmpty());
    }
}
