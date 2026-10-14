package io.ddd4j.javalin.extension.qlexpress;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.extension.qlexpress.QLExpressEngine;
import io.ddd4j.extension.qlexpress.QLExpressEngineBuilder;

/**
 * Guice Module for wiring {@link QLExpressEngine} into Javalin applications.
 *
 * <p>Aligned with ddd4j-boot's {@code Ddd4jQLExpressBootAutoConfiguration}: provides a
 * singleton {@link QLExpressEngine} built via {@link QLExpressEngineBuilder} with safe
 * defaults (built-in functions enabled, isolation security strategy).
 *
 * <p>Consumers can override the engine by binding their own {@link QLExpressEngine}
 * instance via {@code @Provides} in a higher-priority module.
 */
public class Ddd4jQLExpressJavalinModule extends AbstractModule {

    /**
     * 默认构造方法：模块无实例状态，引擎由 {@link #qlExpressEngine()} 惰性提供。
     */
    public Ddd4jQLExpressJavalinModule() {
    }

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    QLExpressEngine qlExpressEngine() {
        return new QLExpressEngineBuilder().build();
    }
}