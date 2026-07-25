package io.ddd4j.javalin.sample.auth.security;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.guice.Ddd4jGuiceModule;
import io.ddd4j.guice.DddAnnotationModule;
import io.ddd4j.javalin.auth.security.Ddd4jSecurityJavalinModule;
import io.javalin.Javalin;

/**
 * ddd4j-javalin-auth + Spring Security 风格示例启动类。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class SecurityAuthApplication {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(
                new Ddd4jGuiceModule(),
                new DddAnnotationModule("io.ddd4j.javalin.sample.auth.security"),
                new Ddd4jSecurityJavalinModule()
        );

        Javalin app = Javalin.create();
        Ddd4jSecurityJavalinModule.registerExceptionHandler(app);
        AuthController controller = injector.getInstance(AuthController.class);
        controller.register(app);
        app.start(8080);
    }

}
