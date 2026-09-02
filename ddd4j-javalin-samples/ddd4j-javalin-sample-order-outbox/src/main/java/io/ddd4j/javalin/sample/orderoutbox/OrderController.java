package io.ddd4j.javalin.sample.orderoutbox;

import com.google.inject.Inject;
import io.ddd4j.core.api.R;
import io.ddd4j.sample.order.application.AddOrderLineCommand;
import io.ddd4j.sample.order.application.CreateOrderCommand;
import io.ddd4j.sample.order.application.OrderApplicationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Order Outbox 示例的显式 Javalin 路由。
 *
 * <p>对齐 ddd4j-boot 的 {@code OrderController}（REST {@code /api/orders}）与
 * rich-model 的 {@code JavalinOrderController}（{@code register(Javalin)} 编程式路由）：
 * <ul>
 *   <li>{@code POST /api/orders} — 创建订单</li>
 *   <li>{@code POST /api/orders/{orderId}/lines} — 添加订单行</li>
 *   <li>{@code GET /api/orders/{orderId}} — 查询订单（读模型投影）</li>
 * </ul>
 * 响应统一使用 {@link R#ok(Object)}。
 */
public class OrderController {

    private final OrderApplicationService applicationService;

    @Inject
    public OrderController(OrderApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
    }

    public void register(Javalin app) {
        app.post("/api/orders", this::create);
        app.post("/api/orders/{orderId}/lines", this::addLine);
        app.get("/api/orders/{orderId}", this::find);
    }

    public void create(Context ctx) {
        CreateOrderRequest request = ctx.bodyAsClass(CreateOrderRequest.class);
        String orderId = applicationService.create(
                new CreateOrderCommand(request.orderNo(), request.buyerId(), request.buyerName())).id();
        ctx.json(R.ok(applicationService.find(orderId)));
    }

    public void addLine(Context ctx) {
        AddOrderLineRequest request = ctx.bodyAsClass(AddOrderLineRequest.class);
        String orderId = ctx.pathParam("orderId");
        applicationService.addLine(new AddOrderLineCommand(orderId, request.goodsId(), request.goodsName(),
                request.quantity(), request.unitPrice()));
        ctx.json(R.ok(applicationService.find(orderId)));
    }

    public void find(Context ctx) {
        ctx.json(R.ok(applicationService.find(ctx.pathParam("orderId"))));
    }

    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }

    public record AddOrderLineRequest(String goodsId, String goodsName, int quantity, BigDecimal unitPrice) {
    }
}
