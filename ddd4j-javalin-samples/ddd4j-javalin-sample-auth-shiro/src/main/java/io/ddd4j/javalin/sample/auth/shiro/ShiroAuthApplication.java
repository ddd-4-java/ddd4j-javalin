package io.ddd4j.javalin.sample.auth.shiro;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.guice.Ddd4jGuiceModule;
import io.ddd4j.guice.DddAnnotationModule;
import io.ddd4j.javalin.auth.shiro.Ddd4jShiroJavalinModule;
import io.javalin.Javalin;

/**
 * ddd4j-javalin-auth + Shiro 示例启动类。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class ShiroAuthApplication {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(
                new Ddd4jGuiceModule(),
                new DddAnnotationModule("io.ddd4j.javalin.sample.auth.shiro"),
                new Ddd4jShiroJavalinModule()
        );

        Javalin app = Javalin.create();
        Ddd4jShiroJavalinModule.registerExceptionHandler(app);
        AuthController controller = injector.getInstance(AuthController.class);
        controller.register(app);
        app.start(8080);
    }

}
