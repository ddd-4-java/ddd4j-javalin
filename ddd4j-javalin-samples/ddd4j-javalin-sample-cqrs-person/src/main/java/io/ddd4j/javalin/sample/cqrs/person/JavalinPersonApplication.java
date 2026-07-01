package io.ddd4j.javalin.sample.cqrs.person;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.guice.Ddd4jGuiceModule;
import io.javalin.Javalin;

public class JavalinPersonApplication {

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
