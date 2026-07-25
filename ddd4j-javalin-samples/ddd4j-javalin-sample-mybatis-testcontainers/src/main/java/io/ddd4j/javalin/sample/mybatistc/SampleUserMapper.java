package io.ddd4j.javalin.sample.mybatistc;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Marker interface for the sample MyBatis-Plus mapper. The actual schema (table
 * {@code sample_user}) is created in the integration test's {@code @BeforeAll}.
 */
@Mapper
public interface SampleUserMapper extends BaseMapper<SampleUserPo> {
}