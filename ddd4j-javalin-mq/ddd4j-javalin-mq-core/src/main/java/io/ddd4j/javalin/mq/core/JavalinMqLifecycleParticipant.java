package io.ddd4j.javalin.mq.core;

import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.health.ReadinessResult;
import io.ddd4j.javalin.core.lifecycle.JavalinLifecycleParticipant;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.event.MQEventStorer;
import io.ddd4j.mq.listener.MQListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 在 Javalin 启动期严格初始化一个 broker client。 */
@Slf4j
public final class JavalinMqLifecycleParticipant implements JavalinLifecycleParticipant {

    private final MQClient client;
    private final MQProperties brokerProperties;
    private final Ddd4jJavalinMqProperties integrationProperties;
    private final Supplier<List<MQListener>> listeners;
    private final MQEventSerialization serialization;
    private final MQEventStorer<MQEvent> storer;
    private Object previousProperties;
    private Object previousSerialization;
    private Object previousStorer;
    private Consumer<MQEvent> previousProducer;
    private Map<String, Consumer<MQEvent>> publishers;
    private boolean createdPublishers;
    private boolean started;
    private boolean closed;

    public JavalinMqLifecycleParticipant(MQClient client, MQProperties brokerProperties,
                                         Ddd4jJavalinMqProperties integrationProperties,
                                         Supplier<List<MQListener>> listeners,
                                         MQEventSerialization serialization,
                                         MQEventStorer<MQEvent> storer) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.brokerProperties = Objects.requireNonNull(brokerProperties, "brokerProperties must not be null");
        this.integrationProperties = Objects.requireNonNull(
                integrationProperties, "integrationProperties must not be null");
        this.listeners = Objects.requireNonNull(listeners, "listeners must not be null");
        this.serialization = Objects.requireNonNull(serialization, "serialization must not be null");
        this.storer = storer;
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public void validate() {
        if (!brokerProperties.isEnabled()) {
            return;
        }
        if (!Objects.equals(brokerProperties.getBroker(), client.impl())) {
            throw new IllegalStateException("MQ broker does not match client: " + brokerProperties.getBroker());
        }
        Ddd4jJavalinMqProperties.Role role = Objects.requireNonNull(
                integrationProperties.getRole(), "MQ role must not be null");
        List<MQListener> discovered = discoveredListeners();
        if (role.consumes() && integrationProperties.isRequireListeners() && discovered.isEmpty()) {
            throw new IllegalStateException("At least one MQ listener is required for consumer role");
        }
        if (brokerProperties.isPersist() && Objects.isNull(storer)) {
            throw new IllegalStateException("MQEventStorer is required when persist=true");
        }
    }

    @Override
    public synchronized void start() {
        if (started) {
            return;
        }
        validate();
        if (!brokerProperties.isEnabled()) {
            started = true;
            return;
        }
        try {
            registerContext();
            Ddd4jJavalinMqProperties.Role role = integrationProperties.getRole();
            if (role.produces()) {
                Consumer<MQEvent> producer = client.initProducer(brokerProperties);
                if (Objects.isNull(producer)) {
                    throw new IllegalStateException("MQ producer initialization returned null: " + client.impl());
                }
                registerProducer(MqContextPropagator.wrap(producer));
            }
            if (role.consumes()) {
                for (MQListener original : discoveredListeners()) {
                    MQListener listener = wrapListener(original);
                    if (!client.initConsumer(listener, brokerProperties)) {
                        throw new IllegalStateException(
                                "MQ consumer initialization returned false: "
                                        + listener.getRouteExpression(client.defaultConcat()));
                    }
                }
            }
            client.start();
            started = true;
        } catch (RuntimeException exception) {
            closeAfterFailedStart();
            throw exception;
        } catch (Exception exception) {
            closeAfterFailedStart();
            throw new IllegalStateException("MQ initialization failed: " + client.impl(), exception);
        }
    }

    @Override
    public synchronized ReadinessResult readiness() {
        if (!brokerProperties.isEnabled()) {
            return ReadinessResult.ready("mq:" + client.impl() + ":disabled");
        }
        return started && !closed
                ? ReadinessResult.ready("mq:" + client.impl())
                : ReadinessResult.unavailable("mq:" + client.impl(), "not started");
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        try {
            client.close();
        } finally {
            restoreContext();
            closed = true;
            started = false;
        }
    }

    private List<MQListener> discoveredListeners() {
        return List.copyOf(Objects.requireNonNull(listeners.get(), "listeners result must not be null"));
    }

    /**
     * 包装 listener bean 以重建消费线程 ThreadContext。
     *
     * <p>JDK {@code Proxy} 包裹原 bean 后保持 listener 引用相等性（route 校验仍按
     * 原 listener），但 invoke 前通过 {@link MqContextPropagator#restore(MQEvent)}
     * 按 {@code msgId} 恢复 publisher 端 capture 的快照，invoke 后自动关闭。
     * 失败（snapshot 缺失 / 反序列化异常）降级为无上下文调用，不影响业务。</p>
     */
    private MQListener wrapListener(MQListener original) {
        Object wrappedBean = MqContextPropagator.wrapListenerBean(original.getBean(), original);
        if (wrappedBean == original.getBean()) {
            return original;
        }
        return MQListener.builder()
                .bean(wrappedBean)
                .method(original.getMethod())
                .group(original.getGroup())
                .namespace(original.getNamespace())
                .topic(original.getTopic())
                .tags(original.getTags())
                .supports(original.supports())
                .separator(original.getSeparator())
                .build();
    }

    private void registerContext() {
        previousProperties = BaseContext.get(MQEvent.MQ_PROPERTIES);
        previousSerialization = BaseContext.get(MQClient.MQ_SERIALIZATION);
        previousStorer = BaseContext.get(MQClient.MQ_STORER);
        BaseContext.inject(MQEvent.MQ_PROPERTIES, brokerProperties);
        BaseContext.inject(MQClient.MQ_SERIALIZATION, serialization);
        if (Objects.nonNull(storer)) {
            BaseContext.inject(MQClient.MQ_STORER, storer);
        }
    }

    private void registerProducer(Consumer<MQEvent> producer) {
        publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
        if (Objects.isNull(publishers)) {
            publishers = new ConcurrentHashMap<>();
            BaseContext.inject(MQEvent.MQ_EVENT_PUBLISHER, publishers);
            createdPublishers = true;
        }
        previousProducer = publishers.put(client.impl(), producer);
    }

    private void closeAfterFailedStart() {
        try {
            client.close();
        } catch (RuntimeException closeException) {
            log.warn("MQ client close after failed startup also failed: {}", client.impl(), closeException);
        } finally {
            restoreContext();
            closed = true;
        }
    }

    private void restoreContext() {
        restore(MQEvent.MQ_PROPERTIES, previousProperties);
        restore(MQClient.MQ_SERIALIZATION, previousSerialization);
        restore(MQClient.MQ_STORER, previousStorer);
        if (Objects.nonNull(publishers)) {
            if (Objects.nonNull(previousProducer)) {
                publishers.put(client.impl(), previousProducer);
            } else {
                publishers.remove(client.impl());
            }
            if (createdPublishers && publishers.isEmpty()) {
                BaseContext.remove(MQEvent.MQ_EVENT_PUBLISHER);
            }
        }
        // 清空 MQ 跨线程上下文快照表，防止长时间运行的进程累积
        // （即便 publisher 崩溃、consumer 永远收不到，registry 也会随生命周期回收）。
        BaseContext.remove(MqContextPropagator.SNAPSHOT_REGISTRY_KEY);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void restore(String key, Object previous) {
        if (Objects.isNull(previous)) {
            BaseContext.remove(key);
        } else {
            BaseContext.inject(key, previous);
        }
    }
}
