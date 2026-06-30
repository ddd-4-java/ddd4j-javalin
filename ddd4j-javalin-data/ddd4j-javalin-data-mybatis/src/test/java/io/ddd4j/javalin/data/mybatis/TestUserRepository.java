package io.ddd4j.javalin.data.mybatis;

import io.ddd4j.core.contract.Model;
import io.ddd4j.core.contract.Query;
import io.ddd4j.data.mybatis.repository.impl.BaseRepositoryImpl;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试用 Repository（BaseRepositoryImpl 子类）。
 *
 * <p>验证 ddd4j-javalin 的 Guice Module 能正确实例化 Repository、注入 Mapper、执行 CRUD。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class TestUserRepository extends BaseRepositoryImpl<TestUserMapper, TestUserRepository.TestUserModel, TestUserPo, TestUserRepository.TestUserQuery> {

    /**
     * 测试用领域模型（直接持有 PO 字段，简化测试）。
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestUserModel extends Model {
        private Long id;
        private String username;
        private String email;
    }

    /**
     * 测试用查询条件。
     */
    public static class TestUserQuery extends Query {
    }
}
