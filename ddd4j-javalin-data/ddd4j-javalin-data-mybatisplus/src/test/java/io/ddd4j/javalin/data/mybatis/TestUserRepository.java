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

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestUserModel extends AggregateRoot<Long> {
        private Long id;
        private String username;
        private String email;

        @Override
        public Long id() {
            return id;
        }
    }

    public static class TestUserQuery extends Query<TestUserModel> {
    }
}
