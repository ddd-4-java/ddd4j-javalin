package io.ddd4j.javalin.sample.auth.satoken;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.guice.core.Ddd4jGuiceModule;
import io.ddd4j.javalin.guice.scan.DddAnnotationModule;
import io.javalin.Javalin;

/**
 * ddd4j-javalin-auth + sa-token 示例启动类。
 *
 * <p>用 Javalin 编程式 API 注册路由 + Guice 管理依赖注入。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class SaTokenAuthApplication {

    public static void main(String[] args) {
        // 1. 创建 Guice Injector：基础设施 + DDD 注解自动扫描绑定
        Injector injector = Guice.createInjector(
                new Ddd4jGuiceModule(),
                new DddAnnotationModule("io.ddd4j.javalin.sample.auth.satoken")
        );

        // 2. 启动 Javalin，用编程式 API 注册路由
        Javalin app = Javalin.create();
        AuthController controller = injector.getInstance(AuthController.class);
        controller.register(app);
        app.start(8080);
    }

}
