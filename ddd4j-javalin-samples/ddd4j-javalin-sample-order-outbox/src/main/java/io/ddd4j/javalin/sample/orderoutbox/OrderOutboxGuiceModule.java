package io.ddd4j.javalin.sample.orderoutbox;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.sample.order.application.IdempotencyPort;
import io.ddd4j.sample.order.application.OrderApplicationService;
import io.ddd4j.sample.order.application.OrderReadModelPort;
import io.ddd4j.sample.order.application.OrderTransactionPort;
import io.ddd4j.sample.order.application.OutboxPort;
import io.ddd4j.sample.order.application.OutboxPublisher;
import io.ddd4j.sample.order.domain.OrderRepository;
import io.ddd4j.sample.order.local.InMemoryOrderAdapters;

/**
 * Order Outbox 示例的默认（in-memory）Guice 装配。
 *
 * <p>使用 {@link InMemoryOrderAdapters}（ddd4j-sample-order-local）同时实现订单仓储、
 * Outbox、读模型、幂等与事务端口；集成事件发布用 {@link RecordingIntegrationEventPublisher}
 * 记录已发布消息。对齐 ddd4j-boot 的 {@code OrderSampleConfiguration}（in-memory 分支）。
 *
 * <p>注意：适配器在模块构造时创建一次，模块可被多个 Injector 复用（例如
 * {@link OrderOutboxApplication#main} 用同一个模块实例启动 web 层并注册路由），
 * 保证所有注入器共享同一订单存储。
 */
public class OrderOutboxGuiceModule extends AbstractModule {

    private final InMemoryOrderAdapters adapters = new InMemoryOrderAdapters();

    @Override
    protected void configure() {
        bind(InMemoryOrderAdapters.class).toInstance(adapters);
        bind(OrderRepository.class).toInstance(adapters);
        bind(OutboxPort.class).toInstance(adapters);
        bind(OrderReadModelPort.class).toInstance(adapters);
        bind(IdempotencyPort.class).toInstance(adapters);
        bind(OrderTransactionPort.class).toInstance(adapters);
    }

    @Provides
    @Singleton
    OrderOutboxSampleProperties orderOutboxSampleProperties() {
        return new OrderOutboxSampleProperties();
    }

    @Provides
    @Singleton
    RecordingIntegrationEventPublisher recordingIntegrationEventPublisher() {
        return new RecordingIntegrationEventPublisher();
    }

    @Provides
    @Singleton
    OutboxPublisher outboxPublisher(OutboxPort outbox, RecordingIntegrationEventPublisher publisher) {
        return new OutboxPublisher(outbox, publisher);
    }

    @Provides
    @Singleton
    OrderApplicationService orderApplicationService(OrderRepository repository, OutboxPort outbox,
                                                    OrderReadModelPort readModels, IdempotencyPort idempotency,
                                                    OrderTransactionPort transaction) {
        return new OrderApplicationService(repository, outbox, readModels, idempotency, transaction);
    }

    @Provides
    @Singleton
    OrderController orderController(OrderApplicationService applicationService) {
        return new OrderController(applicationService);
    }

    @Provides
    @Singleton
    OrderOutboxScheduler orderOutboxScheduler(OutboxPublisher publisher, OrderOutboxSampleProperties properties) {
        return new OrderOutboxScheduler(publisher, properties);
    }
}
