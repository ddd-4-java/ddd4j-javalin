package io.ddd4j.javalin.data.mybatis;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.core.contract.Page;
import io.ddd4j.core.event.TypeHandlerRegistry;
import io.ddd4j.data.mybatis.config.BaseDataProperties;
import org.apache.ibatis.session.SqlSession;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ddd4j-javalin-data-mybatis Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，BaseRepositoryImpl 子类可注入 Mapper 并执行完整 CRUD。
 * 这是"javalin 侧 data 适配真正可用"的端到端证据（H2 内存库 + 建表 + 增删改查）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jMybatisJavalinModuleTest {

    private static DataSource dataSource;
    private static Injector injector;

    @BeforeAll
    static void setUp() throws Exception {
        // H2 内存库
        dataSource = JdbcConnectionPool.create("jdbc:h2:mem:ddd4j_test;DB_CLOSE_DELAY=-1", "sa", "");
        // 建表
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS test_user (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(100), " +
                    "email VARCHAR(100))");
        }

        // 创建 Guice Module：声明 Mapper + 绑定 Repository
        Ddd4jMybatisJavalinModule module = new Ddd4jMybatisJavalinModule(dataSource)
                .bindRepository(TestUserRepository.class, TestUserMapper.class);
        injector = Guice.createInjector(module);
        // 对标 RepositoryBeanPostProcessor：Injector 创建后注入 Mapper 到 Repository
        module.initRepositories(injector);
    }

    @AfterAll
    static void tearDown() {
        if (injector != null) {
            injector.getInstance(SqlSession.class).close();
        }
    }

    /**
     * 验证核心契约可从 Guice 解析。
     */
    @Test
    void shouldResolveCoreContracts() {
        assertNotNull(injector.getInstance(SqlSession.class), "SqlSession 应可注入");
        assertNotNull(injector.getInstance(BaseDataProperties.class),
                "BaseDataProperties 应可注入");
        assertNotNull(injector.getInstance(TypeHandlerRegistry.class),
                "TypeHandlerRegistry 应可注入");
    }

    /**
     * 验证 Repository 可从 Guice 获取且 Mapper 已自动注入（非 null）。
     */
    @Test
    void repositoryShouldHaveMapperInjected() {
        TestUserRepository repo = injector.getInstance(TestUserRepository.class);
        assertNotNull(repo, "Repository 应可注入");
        assertNotNull(repo.getMapper(), "Mapper 应被自动注入（对标 RepositoryBeanPostProcessor）");
    }

    /**
     * 验证完整 CRUD 闭环：插入 → 查询 → 更新 → 删除。
     */
    @Test
    void shouldExecuteCrudOperations() {
        TestUserRepository repo = injector.getInstance(TestUserRepository.class);

        // === CREATE ===
        TestUserRepository.TestUserModel user = new TestUserRepository.TestUserModel();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        TestUserRepository.TestUserModel saved = repo.save(user);
        assertNotNull(saved, "save 应返回保存后的聚合根");
        assertNotNull(user.getId(), "save 后 id 应被回填");

        // === READ ===
        TestUserRepository.TestUserModel found = repo.get(user.getId());
        assertNotNull(found, "get 应返回非 null");
        assertEquals("alice", found.getUsername(), "username 应匹配");
        assertEquals("alice@example.com", found.getEmail(), "email 应匹配");

        // === UPDATE ===
        found.setEmail("alice@updated.com");
        TestUserRepository.TestUserModel updated = repo.update(found);
        assertNotNull(updated, "update 应返回更新后的聚合根");
        TestUserRepository.TestUserModel updatedFound = repo.get(found.getId());
        assertEquals("alice@updated.com", updatedFound.getEmail(), "email 应已更新");

        // === LIST ===
        TestUserRepository.TestUserQuery query = new TestUserRepository.TestUserQuery();
        java.util.List<TestUserRepository.TestUserModel> list = repo.list(query);
        assertFalse(list.isEmpty(), "list 应返回非空");

        // === PAGE ===
        Page<TestUserRepository.TestUserModel> page = repo.page(query);
        assertNotNull(page, "page 应返回非 null");
        assertTrue(page.getTotal() >= 1, "page total 应 >= 1");

        // === DELETE ===
        boolean deleted = repo.delete(user.getId());
        assertTrue(deleted, "delete 应返回 true");
        TestUserRepository.TestUserModel deletedFound = repo.get(user.getId());
        assertNull(deletedFound, "删除后 get 应返回 null");
    }
}
