package io.ddd4j.javalin.sample.orderoutbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.database.PostgresTestContainerFixture;
import io.ddd4j.javalin.web.JavalinTestFixture;
import io.ddd4j.sample.order.application.OutboxDispatchResult;
import io.ddd4j.sample.order.application.OutboxMessage;
import io.ddd4j.sample.order.application.OutboxPublisher;
import io.ddd4j.sample.order.jdbc.JdbcOrderTransactionPort;
import io.ddd4j.sample.order.jdbc.JdbcOutboxPort;
import io.ddd4j.sample.order.jdbc.TransactionalOutboxPublisher;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL 事务 Outbox 的端到端集成测试。
 *
 * <p>用 {@link PostgresTestContainerFixture} 启动真实 PostgreSQL，执行
 * {@code db/schema.sql}（合并自 ddd4j-sample-order-jdbc 的 Flyway V1/V2），
 * 通过 {@link OrderPostgresGuiceModule} + 注入的 {@link DataSource} 完成 Postgres 装配，
 * 然后走完整 create → publish → read round-trip：HTTP 写入订单与行（同事务落库订单表、
 * Outbox 表与读模型表），再调用 {@link TransactionalOutboxPublisher#publishPending(int)}
 * 发布待发送事件并断言数据库中的 PUBLISHED 状态。
 */
@Tag("integration")
@JunitJupiterTestContainers
class OrderOutboxPostgresIT extends JavalinTestFixture {

    private static final ObjectMapper JSON = new ObjectMapper();

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgresTestContainerFixture().newContainer();

    private static final AtomicBoolean SCHEMA_READY = new AtomicBoolean();

    @Override
    protected Module[] extraModules() {
        POSTGRES.start();
        ensureSchema();
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return new Module[]{
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(DataSource.class).toInstance(dataSource);
                    }
                },
                new OrderPostgresGuiceModule()
        };
    }

    @Override
    protected void configureRoutes(Javalin app) {
        injector.getInstance(OrderController.class).register(app);
    }

    @AfterAll
    static void tearDownContainer() {
        if (POSTGRES.isRunning()) {
            POSTGRES.stop();
        }
    }

    @Test
    void shouldRoundTripOrderThroughPostgresAndPublishOutbox() throws Exception {
        // 1. HTTP 创建订单（订单 + Outbox + 读模型在同一 JDBC 事务内落库）
        HttpResponse<String> create = http(HttpRequest.newBuilder(url("/api/orders"))
                .header("Content-Type", "application/json")
                .header("Authorization", OrderOutboxTestSupport.AUTHORIZATION)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"orderNo\":\"ORDER-PG-001\",\"buyerId\":\"buyer-1\",\"buyerName\":\"Alice\"}"))
                .build());
        assertThat(create.statusCode()).isEqualTo(200);
        JsonNode created = JSON.readTree(create.body());
        String orderId = created.path("data").path("id").asText();
        assertThat(orderId).isNotBlank();

        // 2. HTTP 添加订单行
        HttpResponse<String> line = http(HttpRequest.newBuilder(url("/api/orders/" + orderId + "/lines"))
                .header("Content-Type", "application/json")
                .header("Authorization", OrderOutboxTestSupport.AUTHORIZATION)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"goodsId\":\"goods-1\",\"goodsName\":\"DDD Book\",\"quantity\":2,\"unitPrice\":59.90}"))
                .build());
        assertThat(line.statusCode()).isEqualTo(200);
        JsonNode afterLine = JSON.readTree(line.body());
        assertThat(afterLine.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("119.80");
        assertThat(afterLine.path("data").path("status").asText()).isEqualTo("DRAFT");

        // 3. HTTP 查询读模型（PostgreSQL 投影表）
        HttpResponse<String> find = http(HttpRequest.newBuilder(url("/api/orders/" + orderId))
                .header("Authorization", OrderOutboxTestSupport.AUTHORIZATION)
                .GET().build());
        assertThat(find.statusCode()).isEqualTo(200);
        JsonNode found = JSON.readTree(find.body());
        assertThat(found.path("data").path("orderNo").asText()).isEqualTo("ORDER-PG-001");

        // 4. 事务 Outbox 发布：领取 + 发送 + 确认在单个 PostgreSQL 事务内完成
        OutboxDispatchResult result = injector.getInstance(TransactionalOutboxPublisher.class).publishPending(100);
        assertThat(result.failed()).isZero();
        assertThat(result.published()).isEqualTo(2);

        RecordingIntegrationEventPublisher recording = injector.getInstance(RecordingIntegrationEventPublisher.class);
        assertThat(recording.published()).hasSize(2)
                .extracting(OutboxMessage::eventType)
                .containsExactlyInAnyOrder(
                        "io.ddd4j.sample.order.domain.event.OrderCreatedEvent",
                        "io.ddd4j.sample.order.domain.event.OrderLineAddedEvent");

        // 5. 数据库侧断言：Outbox 全部 PUBLISHED、读模型已投影
        DataSource dataSource = injector.getInstance(DataSource.class);
        assertThat(countOutboxStatus(dataSource, "PUBLISHED")).isEqualTo(2);
        assertThat(countOutboxStatus(dataSource, "PENDING")).isZero();
        assertThat(countRows(dataSource, "SELECT COUNT(*) FROM sample_order_read_models")).isEqualTo(1);
    }

    @Test
    void shouldKeepFailedMessagePendingAndPublishItOnRetry() {
        DataSource dataSource = injector.getInstance(DataSource.class);
        JdbcOrderTransactionPort transaction = new JdbcOrderTransactionPort(dataSource);
        JdbcOutboxPort outbox = new JdbcOutboxPort(transaction, JSON);
        OutboxMessage message = new OutboxMessage("retry-message", "retry-order", "RetryEvent",
                java.util.Map.of("value", "retry"), Instant.now());
        transaction.execute(() -> outbox.append(List.of(message)));
        AtomicInteger attempts = new AtomicInteger();
        OutboxPublisher publisher = new OutboxPublisher(outbox, ignored -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("broker unavailable");
            }
        });
        TransactionalOutboxPublisher transactional = new TransactionalOutboxPublisher(transaction, publisher);

        OutboxDispatchResult failed = transactional.publishPending(1);
        assertThat(failed.failed()).isEqualTo(1);
        assertThat(countOutboxStatus(dataSource, "PENDING")).isGreaterThanOrEqualTo(1);
        assertThat(outboxError(dataSource, "retry-message")).isEqualTo("broker unavailable");

        OutboxDispatchResult retried = transactional.publishPending(1);
        assertThat(retried.published()).isEqualTo(1);
        assertThat(outboxStatus(dataSource, "retry-message")).isEqualTo("PUBLISHED");
        assertThat(outboxAttempts(dataSource, "retry-message")).isEqualTo(2);
    }

    private static void ensureSchema() {
        if (!SCHEMA_READY.compareAndSet(false, true)) {
            return;
        }
        String schema = readSchema();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql.trim());
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create Order Outbox schema", exception);
        }
    }

    private static String readSchema() {
        try (var input = OrderOutboxPostgresIT.class.getResourceAsStream("/db/schema.sql")) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource /db/schema.sql");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read /db/schema.sql", exception);
        }
    }

    private static int countOutboxStatus(DataSource dataSource, String status) {
        return countRows(dataSource, "SELECT COUNT(*) FROM sample_order_outbox WHERE status = '" + status + "'");
    }

    private static int countRows(DataSource dataSource, String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getInt(1);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to query sample schema", exception);
        }
    }

    private static String outboxStatus(DataSource dataSource, String id) {
        return outboxValue(dataSource, id, "status");
    }

    private static String outboxError(DataSource dataSource, String id) {
        return outboxValue(dataSource, id, "last_error");
    }

    private static int outboxAttempts(DataSource dataSource, String id) {
        return Integer.parseInt(outboxValue(dataSource, id, "attempts"));
    }

    private static String outboxValue(DataSource dataSource, String id, String column) {
        String sql = "SELECT " + column + " FROM sample_order_outbox WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString(1);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to query outbox message", exception);
        }
    }
}
