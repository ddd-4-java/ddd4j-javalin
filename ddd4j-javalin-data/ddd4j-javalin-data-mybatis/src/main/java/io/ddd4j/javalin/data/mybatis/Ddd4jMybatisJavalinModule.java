package io.ddd4j.javalin.data.mybatis;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.ddd4j.core.contract.TypeHandlerRegistry;
import io.ddd4j.data.mybatis.config.BaseDataProperties;
import io.ddd4j.data.mybatis.typehandler.MybatisTypeHandlerRegistry;

import jakarta.inject.Singleton;

/**
 * ddd4j-javalin MyBatis 数据层 Guice Module。
 *
 * <p>注册 ddd4j-data-mybatis 的核心 Bean 到 Guice 容器：
 * <ul>
 *   <li>{@link BaseDataProperties} — 配置属性</li>
 *   <li>{@link TypeHandlerRegistry} — 类型处理器注册表（MybatisTypeHandlerRegistry）</li>
 * </ul>
 *
 * <p>BaseRepositoryImpl 子类由业务项目通过自定义 Guice Module 注册，
 * mapper 通过 MyBatis SqlSession 手动注入（Javalin 无 Spring 容器自动注入）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jMybatisJavalinModule extends AbstractModule {

    @Provides
    @Singleton
    public BaseDataProperties baseDataProperties() {
        return new BaseDataProperties();
    }

    @Provides
    @Singleton
    public TypeHandlerRegistry typeHandlerRegistry() {
        return new MybatisTypeHandlerRegistry();
    }

}
