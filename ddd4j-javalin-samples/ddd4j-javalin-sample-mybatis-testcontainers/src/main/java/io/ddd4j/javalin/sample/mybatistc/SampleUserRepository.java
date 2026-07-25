package io.ddd4j.javalin.sample.mybatistc;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.session.SqlSession;

import java.io.Serializable;
import java.util.List;

/**
 * Minimal sample repository that mirrors the contract used by
 * {@code io.ddd4j.data.mybatis.repository.BaseRepositoryImpl} without inheriting from
 * it (which would force a hard dependency on ddd4j-data-mybatisplus's internal API).
 *
 * <p>The real sample for end-to-end CRUD lives in
 * {@code ddd4j-javalin-data-mybatisplus/src/test/java/.../Ddd4jMybatisJavalinMySqlIT.java}.
 * This class exists so that {@link MybatisTestcontainersApplication} can wire a
 * concrete {@link SampleUserMapper} + SqlSession without requiring the
 * BaseRepositoryImpl chain.
 */
public class SampleUserRepository {

    private final SqlSession sqlSession;
    private final Class<? extends SampleUserMapper> mapperClass;

    public SampleUserRepository(SqlSession sqlSession, Class<? extends SampleUserMapper> mapperClass) {
        this.sqlSession = sqlSession;
        this.mapperClass = mapperClass;
    }

    public SampleUserPo selectById(Serializable id) {
        return mapper().selectById(id);
    }

    public int insert(SampleUserPo po) {
        return mapper().insert(po);
    }

    public int updateById(SampleUserPo po) {
        return mapper().updateById(po);
    }

    public int deleteById(Serializable id) {
        return mapper().deleteById(id);
    }

    public List<SampleUserPo> selectList(Wrapper<SampleUserPo> wrapper) {
        return mapper().selectList(wrapper);
    }

    public IPage<SampleUserPo> selectPage(IPage<SampleUserPo> page, Wrapper<SampleUserPo> wrapper) {
        return mapper().selectPage(page, wrapper);
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseMapper<SampleUserPo>> T mapper() {
        return (T) sqlSession.getMapper(mapperClass);
    }
}