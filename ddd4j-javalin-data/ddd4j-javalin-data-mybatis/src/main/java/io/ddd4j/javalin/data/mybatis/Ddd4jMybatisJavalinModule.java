package io.ddd4j.javalin.data.mybatis;

import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.core.contract.TypeHandlerRegistry;
import io.ddd4j.data.mybatis.config.BaseDataProperties;
import io.ddd4j.data.mybatis.repository.impl.BaseRepositoryImpl;
import io.ddd4j.data.mybatis.typehandler.MybatisTypeHandlerRegistry;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * ddd4j-javalin MyBatis 数据层 Guice Module。
 *
 * <p>对标 Spring Boot 的 MyBatis-Plus 自动装配（mybatis-plus-spring-boot-starter），
 * 在 Guice 容器中创建 MyBatis 基础设施，让 {@code BaseRepositoryImpl} 子类可用：
 * <ul>
 *   <li>{@link SqlSessionFactory} —— 基于 {@link DataSource} 创建，注册 MyBatis-Plus 分页插件</li>
 *   <li>{@link SqlSession} —— 运行时获取 Mapper 代理（替代 Spring 的 MapperScanner）</li>
 *   <li>{@link BaseDataProperties} —— ddd4j 数据层配置</li>
 *   <li>{@link TypeHandlerRegistry} —— MyBatis 类型处理器注册表（绑定到 core SPI）</li>
 * </ul>
 *
 * <p>对标 Spring 的 {@code RepositoryBeanPostProcessor}：业务方的 {@code BaseRepositoryImpl}
 * 子类通过 {@link #bindRepository(Class, Class)} 声明，Module 自动从 SqlSession 获取 Mapper 代理
 * 并注入到 Repository 实例。由于 Guice 无 BeanPostProcessor，Mapper 注入在
 * {@link #initRepositories(Injector)} 中显式完成（Injector 创建后调用一次）。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * DataSource ds = ...;  // 业务方提供（如 HikariCP）
 * Ddd4jMybatisJavalinModule module = new Ddd4jMybatisJavalinModule(ds)
 *         .bindRepository(UserRepository.class, UserMapper.class);
 * Injector injector = Guice.createInjector(module);
 * module.initRepositories(injector);  // 自动注入 Mapper 到所有声明的 Repository
 * UserRepository repo = injector.getInstance(UserRepository.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jMybatisJavalinModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(Ddd4jMybatisJavalinModule.class);

    private final DataSource dataSource;
    private final Set<Class<?>> mapperInterfaces = new LinkedHashSet<>();
    /** Repository 实现类 → 其所需的 Mapper 接口（用于自动注入 mapper） */
    private final Map<Class<?>, Class<?>> repositoryToMapper = new LinkedHashMap<>();

    /**
     * @param dataSource 数据源（业务方提供，如 HikariCP / Druid）
     */
    public Ddd4jMybatisJavalinModule(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 声明一个 Mapper 接口（对标 Spring 的 @MapperScan）。
     * Module 创建 SqlSessionFactory 时会注册这些接口。
     *
     * @return this（链式调用）
     */
    public Ddd4jMybatisJavalinModule addMapper(Class<?> mapperInterface) {
        this.mapperInterfaces.add(mapperInterface);
        return this;
    }

    /**
     * 绑定一个 {@code BaseRepositoryImpl} 子类到其所需的 Mapper 接口。
     *
     * <p>对标 Spring 的 {@code RepositoryBeanPostProcessor}：{@link #initRepositories(Injector)}
     * 会实例化 Repository，从 SqlSession 获取 Mapper 代理并通过 {@code setMapper} 注入。
     *
     * @param repositoryImpl Repository 实现类（BaseRepositoryImpl 子类）
     * @param mapperInterface 该 Repository 所需的 Mapper 接口
     * @return this（链式调用）
     */
    public Ddd4jMybatisJavalinModule bindRepository(Class<?> repositoryImpl, Class<?> mapperInterface) {
        this.repositoryToMapper.put(repositoryImpl, mapperInterface);
        this.mapperInterfaces.add(mapperInterface);
        return this;
    }

    @Override
    protected void configure() {
        bind(BaseDataProperties.class).in(Singleton.class);
        bind(TypeHandlerRegistry.class).to(MybatisTypeHandlerRegistry.class).in(Singleton.class);
        // 注册 Repository 实现类（单例）
        for (Class<?> repositoryImpl : repositoryToMapper.keySet()) {
            bind(repositoryImpl).in(Singleton.class);
        }
    }

    /**
     * 在 Injector 创建后调用一次：为所有声明的 Repository 注入 Mapper。
     *
     * <p>对标 Spring {@code RepositoryBeanPostProcessor.postProcessAfterInitialization}：
     * 遍历每个 bindRepository 声明，从 Guice 获取 Repository 实例 + SqlSession 获取 Mapper 代理，
     * 调用 {@code setMapper} 完成注入。
     *
     * @param injector 已创建的 Guice Injector
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void initRepositories(Injector injector) {
        SqlSession sqlSession = injector.getInstance(SqlSession.class);
        for (Map.Entry<Class<?>, Class<?>> entry : repositoryToMapper.entrySet()) {
            Class<?> repositoryImpl = entry.getKey();
            Class<?> mapperInterface = entry.getValue();
            try {
                BaseRepositoryImpl repository = (BaseRepositoryImpl) injector.getInstance(repositoryImpl);
                // setMapper 的形参 MP 是泛型 BaseMapper<P>，运行时擦除，用反射绕过泛型检查
                Object mapper = sqlSession.getMapper(mapperInterface);
                java.lang.reflect.Method setMapper = BaseRepositoryImpl.class.getMethod("setMapper", com.baomidou.mybatisplus.core.mapper.BaseMapper.class);
                setMapper.invoke(repository, mapper);
                log.debug("Injected Mapper {} into Repository {}",
                        mapperInterface.getSimpleName(), repositoryImpl.getSimpleName());
            } catch (Exception e) {
                log.warn("Failed to inject Mapper into {}: {}", repositoryImpl.getSimpleName(), e.getMessage());
            }
        }
        log.info("Repository Mapper injection completed: {} repositories", repositoryToMapper.size());
    }

    /**
     * 提供 SqlSessionFactory（单例，对标 Spring 的 SqlSessionFactoryBean）。
     *
     * <p>注册 MyBatis-Plus 分页插件（{@link PaginationInnerInterceptor}），
     * 让 {@code BaseRepositoryImpl.page()} 可用。
     */
    @Provides
    @Singleton
    public SqlSessionFactory sqlSessionFactory() {
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration =
                new com.baomidou.mybatisplus.core.MybatisConfiguration();

        // 注册 Mapper 接口
        for (Class<?> mapperInterface : mapperInterfaces) {
            if (!configuration.hasMapper(mapperInterface)) {
                configuration.addMapper(mapperInterface);
                log.debug("Registered MyBatis Mapper: {}", mapperInterface.getSimpleName());
            }
        }

        // 注册 MyBatis-Plus 分页插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        configuration.addInterceptor(interceptor);

        org.apache.ibatis.mapping.Environment environment = new org.apache.ibatis.mapping.Environment(
                "ddd4j-javalin",
                new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),
                dataSource);
        configuration.setEnvironment(environment);

        SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
        log.info("SqlSessionFactory created: {} mappers registered", mapperInterfaces.size());
        return factory;
    }

    /**
     * 提供 SqlSession（单例，用于运行时获取 Mapper 代理）。
     *
     * <p>对标 Spring 的 {@code SqlSessionTemplate}。Guice 无自动 Mapper 注入，
     * 业务方通过 {@code sqlSession.getMapper(XxxMapper.class)} 获取代理。
     */
    @Provides
    @Singleton
    public SqlSession sqlSession(SqlSessionFactory sqlSessionFactory) {
        return sqlSessionFactory.openSession(true);
    }
}
