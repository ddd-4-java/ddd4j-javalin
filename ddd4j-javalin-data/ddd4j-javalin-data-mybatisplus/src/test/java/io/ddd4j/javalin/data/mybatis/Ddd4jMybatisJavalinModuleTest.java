package io.ddd4j.javalin.data.mybatis;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ibatis.session.SqlSession;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ddd4j-javalin-data-mybatisplus Guice integration test (H2 in-memory).
 *
 * <p>Verifies Guice assembly: {@link Ddd4jMybatisJavalinModule} installs the
 * {@link SqlSession} so the {@link TestUserRepository} can bind itself against
 * the mapper interface. The actual CRUD round-trip is exercised separately by
 * {@code Ddd4jMybatisJavalinMySqlIT} against a Testcontainers-managed MySQL.
 */
class Ddd4jMybatisJavalinModuleTest {

    private static DataSource dataSource;
    private static Injector injector;

    @BeforeAll
    static void setUp() throws Exception {
        // H2 in-memory
        dataSource = JdbcConnectionPool.create("jdbc:h2:mem:ddd4j_test;DB_CLOSE_DELAY=-1", "sa", "");
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS test_user (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(100), " +
                    "email VARCHAR(100))");
        }

        Ddd4jMybatisJavalinModule module = new Ddd4jMybatisJavalinModule(dataSource)
                .bindRepository(TestUserRepository.class, TestUserMapper.class);
        injector = Guice.createInjector(module);
        module.initRepositories(injector);
    }

    @AfterAll
    static void tearDown() {
        if (injector != null) {
            injector.getInstance(SqlSession.class).close();
        }
    }

    @Test
    void shouldResolveCoreContracts() {
        assertNotNull(injector.getInstance(SqlSession.class), "SqlSession must be available");
    }

    @Test
    void shouldWireTestUserRepository() {
        TestUserRepository repo = injector.getInstance(TestUserRepository.class);
        assertNotNull(repo);
        assertNotNull(repo.getBaseMapper());
    }
}
