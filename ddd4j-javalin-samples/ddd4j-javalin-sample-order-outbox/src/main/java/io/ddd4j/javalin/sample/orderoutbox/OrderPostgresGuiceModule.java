package io.ddd4j.javalin.sample.orderoutbox;

import tools.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.sample.order.application.IdempotencyPort;
import io.ddd4j.sample.order.application.OrderApplicationService;
import io.ddd4j.sample.order.application.OrderReadModelPort;
import io.ddd4j.sample.order.application.OutboxPort;
import io.ddd4j.sample.order.application.OutboxPublisher;
import io.ddd4j.sample.order.domain.OrderRepository;
import io.ddd4j.sample.order.jdbc.JdbcOrderReadModelPort;
import io.ddd4j.sample.order.jdbc.JdbcOrderRepository;
import io.ddd4j.sample.order.jdbc.JdbcOrderTransactionPort;
import io.ddd4j.sample.order.jdbc.JdbcOutboxPort;
import io.ddd4j.sample.order.jdbc.TransactionalOutboxPublisher;
import io.ddd4j.sample.order.local.InMemoryOrderAdapters;

import javax.sql.DataSource;

/**
 * Order Outbox 示例的 PostgreSQL 事务写侧 Guice 装配。
 *
 * <p>对齐 ddd4j-boot 的 {@code OrderPostgresInfrastructureConfiguration}（postgres 分支）：
 * 用 JDBC 适配器装配事务边界、订单仓储、Outbox 与读模型，并通过
 * {@link TransactionalOutboxPublisher} 在单个 JDBC 事务内领取 + 发布 + 确认。
 *
 * <p>javalin sample 不引入 Redis 依赖，幂等端口退化为内存实现；集成事件发布用
 * {@link RecordingIntegrationEventPublisher}（生产可替换为 Kafka 发布器）。
 * 调用方必须先绑定 {@link DataSource}（例如集成测试或应用启动装配）。
 */
public class OrderPostgresGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        // DataSource 由调用方绑定；幂等端口使用内存降级（示例无 Redis 依赖）。
    }

    @Provides
    @Singleton
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Provides
    @Singleton
    JdbcOrderTransactionPort orderTransactionPort(DataSource dataSource) {
        return new JdbcOrderTransactionPort(dataSource);
    }

    @Provides
    @Singleton
    OrderRepository orderRepository(JdbcOrderTransactionPort transaction) {
        return new JdbcOrderRepository(transaction);
    }

    @Provides
    @Singleton
    OutboxPort orderOutboxPort(JdbcOrderTransactionPort transaction, ObjectMapper objectMapper) {
        return new JdbcOutboxPort(transaction, objectMapper);
    }

    @Provides
    @Singleton
    OrderReadModelPort orderReadModelPort(JdbcOrderTransactionPort transaction) {
        return new JdbcOrderReadModelPort(transaction);
    }

    @Provides
    @Singleton
    IdempotencyPort orderIdempotencyPort() {
        // 示例无 Redis；内存适配器仅承担幂等端口职责（单实例降级）。
        return new InMemoryOrderAdapters();
    }

    @Provides
    @Singleton
    RecordingIntegrationEventPublisher recordingIntegrationEventPublisher() {
        return new RecordingIntegrationEventPublisher();
    }

    @Provides
    @Singleton
    OutboxPublisher orderOutboxPublisher(OutboxPort outbox, RecordingIntegrationEventPublisher publisher) {
        return new OutboxPublisher(outbox, publisher);
    }

    @Provides
    @Singleton
    TransactionalOutboxPublisher transactionalOutboxPublisher(JdbcOrderTransactionPort transaction,
                                                               OutboxPublisher publisher) {
        return new TransactionalOutboxPublisher(transaction, publisher);
    }

    @Provides
    @Singleton
    OrderOutboxSampleProperties orderOutboxSampleProperties() {
        return new OrderOutboxSampleProperties();
    }

    @Provides
    @Singleton
    OrderOutboxScheduler orderOutboxScheduler(TransactionalOutboxPublisher publisher,
                                              OrderOutboxSampleProperties properties) {
        return new OrderOutboxScheduler(publisher, properties);
    }

    @Provides
    @Singleton
    OrderApplicationService orderApplicationService(OrderRepository repository, OutboxPort outbox,
                                                    OrderReadModelPort readModels, IdempotencyPort idempotency,
                                                    JdbcOrderTransactionPort transaction) {
        return new OrderApplicationService(repository, outbox, readModels, idempotency, transaction);
    }

    @Provides
    @Singleton
    OrderController orderController(OrderApplicationService applicationService) {
        return new OrderController(applicationService);
    }
}
