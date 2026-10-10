package io.ddd4j.javalin.data.mybatis;

import io.ddd4j.core.cqrs.query.Query;
import io.ddd4j.core.ddd.model.AggregateRoot;
import io.ddd4j.data.mybatis.repository.impl.BaseRepositoryImpl;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 使用真实 ddd4j BaseRepositoryImpl 的测试仓储。 */
public class TestUserRepository extends BaseRepositoryImpl<
        TestUserMapper,
        TestUserRepository.TestUserModel,
        TestUserPo,
        TestUserRepository.TestUserQuery,
        Long> {

    /**
     * 测试用户聚合根模型。
     * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
     * @since 7.1.x
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestUserModel extends AggregateRoot<Long> {
        private Long id;
        private String username;
        private String email;

        /**
         * 返回聚合根标识。
         * @return 聚合根标识。
         */
        @Override
        public Long id() {
            return id;
        }
    }

    /**
     * 测试用户查询条件对象。
     * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
     * @since 7.1.x
     */
    public static class TestUserQuery extends Query<TestUserModel> {
    }
}
