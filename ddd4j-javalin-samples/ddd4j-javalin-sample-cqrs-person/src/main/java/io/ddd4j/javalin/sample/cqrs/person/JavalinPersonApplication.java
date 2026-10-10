package io.ddd4j.javalin.sample.cqrs.person;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.guice.Ddd4jGuiceModule;
import io.javalin.Javalin;

/**
 * 人员 CQRS 示例应用入口。
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 7.1.x
 */
public class JavalinPersonApplication {

    /**
     * 示例程序入口。
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(
                new Ddd4jGuiceModule(),
                new JavalinPersonCqrsModule()
        );
        Javalin app = Javalin.create();
        injector.getInstance(JavalinPersonController.class).register(app);
        injector.getInstance(JavalinPersonProjectionScheduler.class).start();
        app.start(8080);
    }
}
