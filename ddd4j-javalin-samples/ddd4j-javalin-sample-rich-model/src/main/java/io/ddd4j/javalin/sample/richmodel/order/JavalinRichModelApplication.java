package io.ddd4j.javalin.sample.richmodel.order;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.guice.Ddd4jGuiceModule;
import io.javalin.Javalin;

/**
 * Javalin entry point for the rich-model sample.
 */
public class JavalinRichModelApplication {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(
                new Ddd4jGuiceModule(),
                new JavalinRichModelModule()
        );
        Javalin app = Javalin.create();
        injector.getInstance(JavalinOrderController.class).register(app);
        app.start(8080);
    }
}
