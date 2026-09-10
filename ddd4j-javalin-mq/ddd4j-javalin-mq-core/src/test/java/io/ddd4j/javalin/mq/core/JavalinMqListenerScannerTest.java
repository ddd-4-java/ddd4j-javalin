package io.ddd4j.javalin.mq.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.listener.MQListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real classpath and Guice listener discovery contract. */
class JavalinMqListenerScannerTest {

    @Test
    void shouldScanAnnotatedGuiceBeansInDeterministicOrder() {
        Injector injector = Guice.createInjector();
        JavalinMqListenerScanner scanner = new JavalinMqListenerScanner();

        List<MQListener> listeners = scanner.scan(injector,
                List.of("io.ddd4j.javalin.mq.core.fixture.valid"));

        assertThat(listeners).extracting(listener -> listener.getMethod().getName())
                .containsExactly("onCreated", "onUpdated");
        assertThat(listeners).extracting(MQListener::getTopic)
                .containsExactly("orders.created", "orders.updated");
    }

    @Test
    void shouldRejectDuplicateRouteExpressions() {
        Injector injector = Guice.createInjector();
        JavalinMqListenerScanner scanner = new JavalinMqListenerScanner();

        assertThatThrownBy(() -> scanner.scan(injector,
                List.of("io.ddd4j.javalin.mq.core.fixture.duplicate")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate MQ route")
                .hasMessageContaining("orders.duplicate");
    }
}
