package io.ddd4j.javalin.sample.orderoutbox;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.google.inject.Module;
import io.ddd4j.javalin.web.JavalinTestFixture;
import io.ddd4j.sample.order.application.OutboxDispatchResult;
import io.ddd4j.sample.order.application.OutboxMessage;
import io.ddd4j.sample.order.application.OutboxPort;
import io.javalin.Javalin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内存装配的全流程集成测试：真实 HTTP 创建订单 → 添加订单行 → 查询读模型，
 * 再确定性触发 Outbox 发布并断言领域事件已发布、待发布队列清空。
 *
 * <p>通过 {@link JavalinTestFixture} 随机端口启动 Javalin（含 ddd4j 统一请求生命周期），
 * Guice 装配使用 {@link OrderOutboxGuiceModule}（InMemoryOrderAdapters）。
 */
@Tag("integration")
class OrderOutboxInMemoryIT extends JavalinTestFixture {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    protected Module[] extraModules() {
        return new Module[]{new OrderOutboxGuiceModule()};
    }

    @Override
    protected void configureRoutes(Javalin app) {
        injector.getInstance(OrderController.class).register(app);
    }

    @Test
    void shouldCreateAddLineFindAndPublishOutboxEvents() throws Exception {
        // 1. 创建订单
        HttpResponse<String> create = http(HttpRequest.newBuilder(url("/api/orders"))
                .header("Content-Type", "application/json")
                .header("Authorization", OrderOutboxTestSupport.AUTHORIZATION)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"orderNo\":\"ORDER-INMEM-001\",\"buyerId\":\"buyer-1\",\"buyerName\":\"Alice\"}"))
                .build());
        assertThat(create.statusCode()).isEqualTo(200);
        JsonNode created = JSON.readTree(create.body());
        assertThat(created.path("code").asInt()).isEqualTo(0);
        String orderId = created.path("data").path("id").asText();
        assertThat(orderId).isNotBlank();

        // 2. 添加订单行
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

        // 3. 查询读模型
        HttpResponse<String> find = http(HttpRequest.newBuilder(url("/api/orders/" + orderId))
                .header("Authorization", OrderOutboxTestSupport.AUTHORIZATION)
                .GET().build());
        assertThat(find.statusCode()).isEqualTo(200);
        JsonNode found = JSON.readTree(find.body());
        assertThat(found.path("data").path("orderNo").asText()).isEqualTo("ORDER-INMEM-001");
        assertThat(found.path("data").path("buyerId").asText()).isEqualTo("buyer-1");

        // 4. 触发 Outbox 发布（测试中不启动调度线程，手动调用保持确定性）
        OutboxDispatchResult result = injector.getInstance(OrderOutboxScheduler.class).publishPending();
        assertThat(result.published()).isEqualTo(2);
        assertThat(result.failed()).isZero();

        RecordingIntegrationEventPublisher recording = injector.getInstance(RecordingIntegrationEventPublisher.class);
        assertThat(recording.published()).hasSize(2)
                .extracting(OutboxMessage::eventType)
                .containsExactlyInAnyOrder(
                        "io.ddd4j.sample.order.domain.event.OrderCreatedEvent",
                        "io.ddd4j.sample.order.domain.event.OrderLineAddedEvent");

        assertThat(injector.getInstance(OutboxPort.class).pending(10)).isEmpty();
    }
}
