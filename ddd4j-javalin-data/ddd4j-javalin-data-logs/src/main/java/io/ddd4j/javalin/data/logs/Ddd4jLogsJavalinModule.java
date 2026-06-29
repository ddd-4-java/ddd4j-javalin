package io.ddd4j.javalin.data.logs;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.ddd4j.data.logs.aspect.ApiOperationLogProvider;
import io.ddd4j.data.logs.aspect.DefaultApiOperationLogProvider;

import jakarta.inject.Singleton;

/**
 * ddd4j-javalin API 操作日志 Guice Module。
 *
 * <p>注册 ApiOperationLogProvider 到 Guice 容器，替代 Spring Boot 的 auto-config。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jLogsJavalinModule extends AbstractModule {

    @Provides
    @Singleton
    public ApiOperationLogProvider apiOperationLogProvider() {
        return new DefaultApiOperationLogProvider();
    }

}
