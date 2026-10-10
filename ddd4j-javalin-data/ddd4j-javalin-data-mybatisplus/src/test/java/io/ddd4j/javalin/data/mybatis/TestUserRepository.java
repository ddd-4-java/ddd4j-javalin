package io.ddd4j.javalin.data.mybatis;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import org.apache.ibatis.session.SqlSession;

import java.io.Serializable;
import java.util.List;

/**
 * Test repository mirroring the contract used by
 * {@code io.ddd4j.data.mybatis.repository.BaseRepositoryImpl} (save / get / list / page /
 * delete / update / getMapper). The integration test {@code Ddd4jMybatisJavalinMySqlIT}
 * exercises this contract end-to-end against a real MySQL container.
 */
public class TestUserRepository {

    private final SqlSession sqlSession;
    private final Class<? extends TestUserMapper> mapperClass;

    public TestUserRepository(SqlSession sqlSession, Class<? extends TestUserMapper> mapperClass) {
        this.sqlSession = sqlSession;
        this.mapperClass = mapperClass;
    }

    public TestUserModel selectById(Serializable id) {
        return toModel(mapper().selectById(id));
    }

    public TestUserModel save(TestUserModel model) {
        TestUserPo po = toPo(model);
        mapper().insert(po);
        // MyBatis-Plus writes back the auto-generated id into the PO instance;
        // reflect that into our model so the test can reference it.
        if (po.getId() != null) {
            model.setId(po.getId());
        }
        return model;
    }

    public TestUserModel update(TestUserModel model) {
        mapper().updateById(toPo(model));
        return model;
    }

    public boolean delete(Serializable id) {
        return mapper().deleteById(id) > 0;
    }

    public List<TestUserModel> list(TestUserQuery query) {
        return mapper().selectList(null).stream().map(this::toModel).toList();
    }

    public Page<TestUserModel> page(TestUserQuery query) {
        IPage<TestUserPo> mpPage = mapper().selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10),
                null);
        return new Page<>(mpPage.getRecords().stream().map(this::toModel).toList(),
                mpPage.getTotal());
    }

    public TestUserMapper getMapper() {
        return mapper();
    }

    @SuppressWarnings("unchecked")
    private TestUserMapper mapper() {
        return (TestUserMapper) sqlSession.getMapper(mapperClass);
    }

    private TestUserPo toPo(TestUserModel m) {
        TestUserPo po = new TestUserPo();
        po.setId(m.getId());
        po.setUsername(m.getUsername());
        po.setEmail(m.getEmail());
        return po;
    }

    private TestUserModel toModel(TestUserPo po) {
        if (po == null) {
            return null;
        }
        TestUserModel m = new TestUserModel();
        m.setId(po.getId());
        m.setUsername(po.getUsername());
        m.setEmail(po.getEmail());
        return m;
    }

    @Data
    public static class TestUserModel {
        private Long id;
        private String username;
        private String email;
    }

    public static class TestUserQuery {
    }

    /**
     * Minimal Page wrapper (mirrors the {@code Page<T>} contract).
     */
    public final static class Page<T> {

        private static final long serialVersionUID = 0L;

        private final List<T> records;

        private final long total;

        public List<T> getRecords() {
            return records;
        }

        @JsonCreator()
        public Page(@JsonProperty("records") List<T> records, @JsonProperty("total") long total) {
            this.records = records;
            this.total = total;
        }

        @JsonProperty("records")
        public List<T> records() {
            return records;
        }

        @JsonProperty("total")
        public long total() {
            return total;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            Page<?> other = (Page<?>) obj;
            return Objects.equals(this.records, other.records) && this.total == other.total;
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(records);
            result = 31 * result + Long.hashCode(total);
            return result;
        }

        @Override
        public String toString() {
            return "Page[records=" + records + ", total=" + total + "]";
        }
    }
}