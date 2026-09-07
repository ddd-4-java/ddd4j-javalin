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
 *
 * <p>管理 MyBatis-Plus 的 Guice 集成，包括 DataSource、SqlSessionFactory 初始化、
 * Mapper 接口注册，以及 Repository 实现与 Mapper 的自动注入。ddd4j 1.0.x 尚未
 * 发布该适配类型，因此由 Javalin 数据模块提供最小兼容实现。</p>
 */
@Slf4j
public class Ddd4jMybatisGuiceModule extends AbstractModule {

    private final DataSource dataSource;
    private final Set<Class<?>> mapperInterfaces = new LinkedHashSet<>();
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
     * @return 当前模块
     */
    public Ddd4jMybatisGuiceModule addMapper(Class<?> mapperInterface) {
        mapperInterfaces.add(mapperInterface);
        return this;
    }

    /**
     * 绑定 Repository 实现与 Mapper 接口。
     *
     * @param repositoryImpl Repository 实现类
     * @param mapperInterface Mapper 接口类
     * @return 当前模块
     */
    public Ddd4jMybatisGuiceModule bindRepository(Class<?> repositoryImpl, Class<?> mapperInterface) {
        repositoryToMapper.put(repositoryImpl, mapperInterface);
        mapperInterfaces.add(mapperInterface);
        return this;
    }

    @Override
    protected void configure() {
        for (Class<?> repositoryImpl : repositoryToMapper.keySet()) {
            bind(repositoryImpl).in(Singleton.class);
        }
    }

    /**
     * 将已注册 Mapper 注入对应 Repository。
     *
     * @param injector Guice Injector
     */
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
            } catch (ReflectiveOperationException | RuntimeException exception) {
                log.warn("Failed to inject mapper into {}: {}",
                        repositoryImpl.getSimpleName(), exception.getMessage());
            }
        }
        log.info("Repository mapper injection completed: {} repositories", repositoryToMapper.size());
    }

    /**
     * 创建并配置 SqlSessionFactory。
     *
     * @return SqlSessionFactory
     */
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
        configuration.setEnvironment(new Environment(
                "ddd4j-runtime-guice", new JdbcTransactionFactory(), dataSource));

        SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
        log.info("SqlSessionFactory created: {} mappers registered", mapperInterfaces.size());
        return factory;
    }

    /**
     * 创建自动提交的 SqlSession。
     *
     * @param sqlSessionFactory SqlSessionFactory
     * @return SqlSession
     */
    @Provides
    @Singleton
    public SqlSession sqlSession(SqlSessionFactory sqlSessionFactory) {
        return sqlSessionFactory.openSession(true);
    }
}
