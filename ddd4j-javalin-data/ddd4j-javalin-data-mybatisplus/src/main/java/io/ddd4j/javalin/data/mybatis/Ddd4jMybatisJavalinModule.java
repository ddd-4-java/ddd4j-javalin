package io.ddd4j.javalin.data.mybatis;

import io.ddd4j.guice.Ddd4jMybatisGuiceModule;

import javax.sql.DataSource;

/**
 * ddd4j-javalin MyBatis 数据层 Guice Module。
 *
 * <p>对标 Spring Boot 的 MyBatis-Plus 自动装配（mybatis-plus-spring-boot-starter），
 * 在 Guice 容器中创建 MyBatis 基础设施，让 {@code BaseRepositoryImpl} 子类可用：
 * <ul>
 *   <li>{@link SqlSessionFactory} —— 基于 {@link DataSource} 创建，注册 MyBatis-Plus 分页插件</li>
 *   <li>{@link SqlSession} —— 运行时获取 Mapper 代理（替代 Spring 的 MapperScanner）</li>
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
@Deprecated
public class Ddd4jMybatisJavalinModule extends Ddd4jMybatisGuiceModule {

    /**
     * @param dataSource 数据源（业务方提供，如 HikariCP / Druid）
     */
    public Ddd4jMybatisJavalinModule(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * 声明一个 Mapper 接口（对标 Spring 的 @MapperScan）。
     * Module 创建 SqlSessionFactory 时会注册这些接口。
     *
     * @return this（链式调用）
     */
    public Ddd4jMybatisJavalinModule addMapper(Class<?> mapperInterface) {
        super.addMapper(mapperInterface);
        return this;
    }

    /**
     * 绑定一个 {@code BaseRepositoryImpl} 子类到其所需的 Mapper 接口。
     *
     * <p>对标 Spring 的 {@code RepositoryBeanPostProcessor}：{@link #initRepositories(Injector)}
     * 会实例化 Repository，从 SqlSession 获取 Mapper 代理并通过 {@code setMapper} 注入。
     *
     * @param repositoryImpl  Repository 实现类（BaseRepositoryImpl 子类）
     * @param mapperInterface 该 Repository 所需的 Mapper 接口
     * @return this（链式调用）
     */
    public Ddd4jMybatisJavalinModule bindRepository(Class<?> repositoryImpl, Class<?> mapperInterface) {
        super.bindRepository(repositoryImpl, mapperInterface);
        return this;
    }
}
