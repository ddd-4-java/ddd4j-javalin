package io.ddd4j.javalin.data.mybatis.it;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.data.mybatis.Ddd4jMybatisJavalinModule;
import io.ddd4j.javalin.data.mybatis.TestUserMapper;
import io.ddd4j.javalin.data.mybatis.TestUserRepository;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.database.MySqlTestContainerFixture;
import org.apache.ibatis.session.SqlSession;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@link Ddd4jMybatisJavalinModule} against a real MySQL 8 instance
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
                .addMapper(TestUserMapper.class);
        injector = Guice.createInjector(module);

        // Wire the TestUserRepository manually against the SqlSession because
        // Ddd4jMybatisJavalinModule is a thin bridge over the core
        // ddd4j-data-mybatisplus (which exposes BaseRepositoryImpl).
        SqlSession sqlSession = injector.getInstance(SqlSession.class);
        TestUserRepository repo = new TestUserRepository(sqlSession, TestUserMapper.class);
        // Stash the repo via a small Guice module so injector.getInstance resolves it.
        injector = injector.createChildInjector(binder ->
                binder.bind(TestUserRepository.class).toInstance(repo));
    }

    @AfterAll
    static void tearDown() {
        if (injector != null) {
            injector.getInstance(SqlSession.class).close();
        }
        MYSQL.stop();
    }

    @Test
    void shouldResolveCoreContracts() {
        assertNotNull(injector.getInstance(SqlSession.class), "SqlSession must be available");
    }

    @Test
    void shouldExecuteFullCrudFlowAgainstMysql() {
        TestUserRepository repo = injector.getInstance(TestUserRepository.class);
        assertNotNull(repo);
        assertNotNull(repo.getMapper());

        // CREATE
        TestUserRepository.TestUserModel user = new TestUserRepository.TestUserModel();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        TestUserRepository.TestUserModel saved = repo.save(user);
        assertNotNull(saved);
        assertNotNull(user.getId());

        // READ
        TestUserRepository.TestUserModel found = repo.selectById(user.getId());
        assertNotNull(found);
        assertEquals("alice", found.getUsername());
        assertEquals("alice@example.com", found.getEmail());

        // UPDATE
        found.setEmail("alice@updated.com");
        TestUserRepository.TestUserModel updated = repo.update(found);
        assertNotNull(updated);
        TestUserRepository.TestUserModel reloaded = repo.selectById(found.getId());
        assertEquals("alice@updated.com", reloaded.getEmail());

        // LIST
        TestUserRepository.TestUserQuery query = new TestUserRepository.TestUserQuery();
        List<TestUserRepository.TestUserModel> list = repo.list(query);
        assertFalse(list.isEmpty());

        // PAGE
        TestUserRepository.Page<TestUserRepository.TestUserModel> page = repo.page(query);
        assertNotNull(page);
        assertTrue(page.total() >= 1);

        // DELETE
        assertTrue(repo.delete(user.getId()));
        assertNull(repo.selectById(user.getId()));
    }
}