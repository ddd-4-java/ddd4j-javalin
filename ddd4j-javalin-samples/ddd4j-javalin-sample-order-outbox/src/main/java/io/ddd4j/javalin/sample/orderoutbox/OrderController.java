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

    /**
     * 构造订单控制器。
     *
     * @param applicationService 订单应用服务
     */
    @Inject
    public OrderController(OrderApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
    }

    /**
     * 用 Javalin 编程式 API 注册订单路由。
     *
     * @param app Javalin 应用实例
     */
    public void register(Javalin app) {
        app.post("/api/orders", this::create);
        app.post("/api/orders/{orderId}/lines", this::addLine);
        app.get("/api/orders/{orderId}", this::find);
    }

    /**
     * 创建订单：POST /api/orders。
     *
     * @param ctx Javalin 请求上下文
     */
    public void create(Context ctx) {
        CreateOrderRequest request = ctx.bodyAsClass(CreateOrderRequest.class);
        String orderId = applicationService.create(
                new CreateOrderCommand(request.orderNo(), request.buyerId(), request.buyerName())).id();
        ctx.json(R.ok(applicationService.find(orderId)));
    }

    /**
     * 添加订单行：POST /api/orders/{orderId}/lines。
     *
     * @param ctx Javalin 请求上下文
     */
    public void addLine(Context ctx) {
        AddOrderLineRequest request = ctx.bodyAsClass(AddOrderLineRequest.class);
        String orderId = ctx.pathParam("orderId");
        applicationService.addLine(new AddOrderLineCommand(orderId, request.goodsId(), request.goodsName(),
                request.quantity(), request.unitPrice()));
        ctx.json(R.ok(applicationService.find(orderId)));
    }

    /**
     * 查询订单：GET /api/orders/{orderId}（读模型投影）。
     *
     * @param ctx Javalin 请求上下文
     */
    public void find(Context ctx) {
        ctx.json(R.ok(applicationService.find(ctx.pathParam("orderId"))));
    }

    /**
     * 创建订单请求载荷。
     *
     * @param orderNo   订单号
     * @param buyerId   买家 ID
     * @param buyerName 买家名称
     */
    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }

    /**
     * 添加订单行请求载荷。
     *
     * @param goodsId   商品 ID
     * @param goodsName 商品名称
     * @param quantity  数量
     * @param unitPrice 单价
     */
    public record AddOrderLineRequest(String goodsId, String goodsName, int quantity, BigDecimal unitPrice) {
    }
}
