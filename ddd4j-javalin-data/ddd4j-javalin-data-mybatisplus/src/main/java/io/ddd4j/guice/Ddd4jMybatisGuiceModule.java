package io.ddd4j.guice;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.data.mybatis.plugins.inner.Ddd4jAggregateFillInnerInterceptor;
import io.ddd4j.data.mybatis.repository.impl.BaseRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * ddd4j MyBatis 的 Guice 桥接模块。
 * <p>
 * 管理 MyBatis-Plus 的 Guice 集成，包括 DataSource、SqlSessionFactory 的初始化，
 * Mapper 接口注册，以及 Repository 实现与 Mapper 的自动注入。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j
public class Ddd4jMybatisGuiceModule extends AbstractModule {

    /**
     * 数据源
     */
    private final DataSource dataSource;
    /**
     * 注册的 Mapper 接口集
     */
    private final Set<Class<?>> mapperInterfaces = new LinkedHashSet<>();
    /**
     * Repository 实现类到 Mapper 接口的映射
     */
    private final Map<Class<?>, Class<?>> repositoryToMapper = new LinkedHashMap<>();

    /**
     * 创建 MyBatis Guice 桥接模块。
     *
     * @param dataSource 数据源
     */
    public Ddd4jMybatisGuiceModule(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 注册 Mapper 接口。
     *
     * @param mapperInterface Mapper 接口类
     * @return 当前模块（链式调用）
     */
    public Ddd4jMybatisGuiceModule addMapper(Class<?> mapperInterface) {
        this.mapperInterfaces.add(mapperInterface);
        return this;
    }

    /**
     * 绑定 Repository 实现与 Mapper 接口的对应关系。
     *
     * @param repositoryImpl  Repository 实现类
     * @param mapperInterface Mapper 接口类
     * @return 当前模块（链式调用）
     */
    public Ddd4jMybatisGuiceModule bindRepository(Class<?> repositoryImpl, Class<?> mapperInterface) {
        this.repositoryToMapper.put(repositoryImpl, mapperInterface);
        this.mapperInterfaces.add(mapperInterface);
        return this;
    }

    @Override
    protected void configure() {
        for (Class<?> repositoryImpl : repositoryToMapper.keySet()) {
            bind(repositoryImpl).in(Singleton.class);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void initRepositories(Injector injector) {
        SqlSession sqlSession = injector.getInstance(SqlSession.class);
        for (Map.Entry<Class<?>, Class<?>> entry : repositoryToMapper.entrySet()) {
            Class<?> repositoryImpl = entry.getKey();
            Class<?> mapperInterface = entry.getValue();
            try {
                BaseRepositoryImpl repository = (BaseRepositoryImpl) injector.getInstance(repositoryImpl);
                Object mapper = sqlSession.getMapper(mapperInterface);
                Method setMapper = BaseRepositoryImpl.class.getMethod("setBaseMapper", BaseMapper.class);
                setMapper.invoke(repository, mapper);
                log.debug("Injected mapper {} into repository {}",
                        mapperInterface.getSimpleName(), repositoryImpl.getSimpleName());
            } catch (Exception exception) {
                log.warn("Failed to inject mapper into {}: {}", repositoryImpl.getSimpleName(),
                        exception.getMessage());
            }
        }
        log.info("Repository mapper injection completed: {} repositories", repositoryToMapper.size());
    }

    @Provides
    @Singleton
    public SqlSessionFactory sqlSessionFactory() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        for (Class<?> mapperInterface : mapperInterfaces) {
            if (!configuration.hasMapper(mapperInterface)) {
                configuration.addMapper(mapperInterface);
                log.debug("Registered MyBatis mapper: {}", mapperInterface.getSimpleName());
            }
        }

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        configuration.addInterceptor(interceptor);
        configuration.addInterceptor(new Ddd4jAggregateFillInnerInterceptor());
        configuration.setEnvironment(new Environment("ddd4j-runtime-guice", new JdbcTransactionFactory(), dataSource));

        SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
        log.info("SqlSessionFactory created: {} mappers registered", mapperInterfaces.size());
        return factory;
    }

    @Provides
    @Singleton
    public SqlSession sqlSession(SqlSessionFactory sqlSessionFactory) {
        return sqlSessionFactory.openSession(true);
    }
}
