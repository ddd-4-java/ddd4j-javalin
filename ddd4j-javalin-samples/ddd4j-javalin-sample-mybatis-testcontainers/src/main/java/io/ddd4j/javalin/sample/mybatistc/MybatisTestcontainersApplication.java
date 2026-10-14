package io.ddd4j.javalin.sample.mybatistc;

import io.ddd4j.javalin.data.mybatis.Ddd4jMybatisJavalinModule;
import io.ddd4j.javalin.web.Ddd4jJavalinApplication;
import io.javalin.Javalin;

import javax.sql.DataSource;

/**
 * Sample entry point demonstrating the {@code ddd4j-javalin 6.3.x} bootstrap pattern.
 *
 * <ol>
 *   <li>{@link Ddd4jJavalinApplication#run(String[], String, com.google.inject.Module...)}
 *       wires the Guice Injector and starts Javalin on the requested port.</li>
 *   <li>Sample-specific modules (a {@link Ddd4jMybatisJavalinModule} bound to a
 *       Testcontainers-managed DataSource) are passed in.</li>
 *   <li>Application-specific routes are registered after {@code run} returns.</li>
 * </ol>
 */
public final class MybatisTestcontainersApplication {

    private MybatisTestcontainersApplication() {
    }

    /**
     * 启动示例应用：以 Testcontainers 数据源装配 MyBatis 模块并启动 Javalin。
     *
     * @param dataSource 测试容器提供的数据源
     * @param args 传给 {@link Ddd4jJavalinApplication#run(String[], String, com.google.inject.Module...)} 的启动参数
     * @return 已启动的 Javalin 实例
     */
    public static Javalin run(DataSource dataSource, String... args) {
        Ddd4jMybatisJavalinModule mybatisModule = new Ddd4jMybatisJavalinModule(dataSource)
                .addMapper(SampleUserMapper.class);
        return Ddd4jJavalinApplication.run(
                args, "io.ddd4j.javalin.sample.mybatistc", mybatisModule);
    }
}