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

    /**
     * 构造 SampleUserRepository 实例并初始化依赖。
     * @param sqlSession MyBatis 会话。
     * @param mapperClass Mapper 接口类型。
     */
    public SampleUserRepository(SqlSession sqlSession, Class<? extends SampleUserMapper> mapperClass) {
        this.sqlSession = sqlSession;
        this.mapperClass = mapperClass;
    }

    /**
     * 按主键查询记录。
     * @param id 标识值。
     * @return 主键对应的记录；不存在时返回 {@code null}。
     */
    public SampleUserPo selectById(Serializable id) {
        return mapper().selectById(id);
    }

    /**
     * 插入记录。
     * @param po 持久化对象。
     * @return 受影响的行数。
     */
    public int insert(SampleUserPo po) {
        return mapper().insert(po);
    }

    /**
     * 按主键更新记录。
     * @param po 持久化对象。
     * @return 受影响的行数。
     */
    public int updateById(SampleUserPo po) {
        return mapper().updateById(po);
    }

    /**
     * 按主键删除记录。
     * @param id 标识值。
     * @return 受影响的行数。
     */
    public int deleteById(Serializable id) {
        return mapper().deleteById(id);
    }

    /**
     * 按条件查询记录列表。
     * @param wrapper 查询条件包装器。
     * @return 记录列表。
     */
    public List<SampleUserPo> selectList(Wrapper<SampleUserPo> wrapper) {
        return mapper().selectList(wrapper);
    }

    /**
     * 按条件分页查询记录。
     * @param page 分页对象。
     * @param wrapper 查询条件包装器。
     * @return 分页结果。
     */
    public IPage<SampleUserPo> selectPage(IPage<SampleUserPo> page, Wrapper<SampleUserPo> wrapper) {
        return mapper().selectPage(page, wrapper);
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseMapper<SampleUserPo>> T mapper() {
        return (T) sqlSession.getMapper(mapperClass);
    }
}