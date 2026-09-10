package io.ddd4j.javalin.mq.core;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import io.ddd4j.javalin.core.lifecycle.Ddd4jJavalinBootstrapContext;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.event.MQEventStorer;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 构造具有当前 Injector 和扫描包上下文的 MQ 生命周期参与者。 */
final class JavalinMqLifecycleParticipantProvider implements Provider<JavalinMqLifecycleParticipant> {

    private final Injector injector;
    private final MQClient client;
    private final MQProperties brokerProperties;
    private final Ddd4jJavalinMqProperties integrationProperties;
    private final JavalinMqListenerScanner scanner;

    @Inject
    JavalinMqLifecycleParticipantProvider(Injector injector, MQClient client, MQProperties brokerProperties,
                                          Ddd4jJavalinMqProperties integrationProperties,
                                          JavalinMqListenerScanner scanner) {
        this.injector = injector;
        this.client = client;
        this.brokerProperties = brokerProperties;
        this.integrationProperties = integrationProperties;
        this.scanner = scanner;
    }

    @Override
    public JavalinMqLifecycleParticipant get() {
        return new JavalinMqLifecycleParticipant(
                client,
                brokerProperties,
                integrationProperties,
                () -> scanner.scan(injector, bootstrapContext().basePackages()),
                new JsonMQEventSerialization(),
                storer());
    }

    private Ddd4jJavalinBootstrapContext bootstrapContext() {
        Binding<Ddd4jJavalinBootstrapContext> binding = injector.getExistingBinding(
                Key.get(Ddd4jJavalinBootstrapContext.class));
        return Objects.isNull(binding)
                ? new Ddd4jJavalinBootstrapContext(List.of())
                : binding.getProvider().get();
    }

    @SuppressWarnings("unchecked")
    private MQEventStorer<MQEvent> storer() {
        List<Binding<?>> bindings = new ArrayList<>();
        for (java.util.Map.Entry<Key<?>, Binding<?>> entry : injector.getAllBindings().entrySet()) {
            if (MQEventStorer.class.isAssignableFrom(entry.getKey().getTypeLiteral().getRawType())) {
                bindings.add(entry.getValue());
            }
        }
        if (bindings.size() > 1) {
            throw new IllegalStateException("Only one MQEventStorer binding is supported");
        }
        return bindings.isEmpty() ? null : (MQEventStorer<MQEvent>) bindings.get(0).getProvider().get();
    }
}
