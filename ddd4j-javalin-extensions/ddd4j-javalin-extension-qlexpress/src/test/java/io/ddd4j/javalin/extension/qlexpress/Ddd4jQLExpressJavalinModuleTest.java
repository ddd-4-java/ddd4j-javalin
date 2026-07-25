package io.ddd4j.javalin.extension.qlexpress;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.extension.qlexpress.QLExpressEngine;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link Ddd4jQLExpressJavalinModule} resolves a working
 * {@link QLExpressEngine} and that the built-in arithmetic function works as expected.
 */
class Ddd4jQLExpressJavalinModuleTest {

    @Test
    void shouldResolveQlExpressEngineFromGuice() {
        Injector injector = Guice.createInjector(new Ddd4jQLExpressJavalinModule());
        QLExpressEngine engine = injector.getInstance(QLExpressEngine.class);

        assertThat(engine).isNotNull();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("a", 1);
        ctx.put("b", 2);
        Object result = engine.execute("a + b", ctx, Integer.class);
        assertThat(result).isEqualTo(3);
    }
}