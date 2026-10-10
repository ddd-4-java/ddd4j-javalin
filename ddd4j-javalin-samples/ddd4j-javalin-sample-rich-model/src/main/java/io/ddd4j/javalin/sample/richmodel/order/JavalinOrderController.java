package io.ddd4j.javalin.sample.richmodel.order;

import com.google.inject.Inject;
import io.ddd4j.sample.richmodel.order.application.AddOrderLineCommand;
import io.ddd4j.sample.richmodel.order.application.CreateOrderCommand;
import io.ddd4j.sample.richmodel.order.application.OrderApplicationService;
import io.ddd4j.sample.richmodel.order.domain.model.Order;
import io.ddd4j.sample.richmodel.order.domain.repository.OrderRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Javalin route adapter for the rich-model sample.
 */
public class JavalinOrderController {

    private final OrderApplicationService applicationService;
    private final OrderRepository repository;

    /**
     * 构造 JavalinOrderController 并注入依赖。
     * @param applicationService 订单应用服务。
     * @param repository 仓储实例。
     */
    @Inject
    public JavalinOrderController(OrderApplicationService applicationService, OrderRepository repository) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * 注册订单路由。
     * @param app Javalin 应用实例。
     */
    public void register(Javalin app) {
        app.unsafe.routes.post("/orders", this::create);
        app.unsafe.routes.post("/orders/{orderId}/lines", this::addLine);
        app.unsafe.routes.post("/orders/{orderId}/pay", this::pay);
        app.unsafe.routes.get("/orders/by-no/{orderNo}", this::findByOrderNo);
    }

    /**
     * 创建订单草稿。
     * @param ctx Javalin 请求上下文。
     */
    public void create(Context ctx) {
        CreateOrderRequest request = ctx.bodyAsClass(CreateOrderRequest.class);
        Order order = applicationService.createDraft(new CreateOrderCommand(
                request.orderNo(),
                request.buyerId(),
                request.buyerName()
        ));
        ctx.json(OrderResponse.from(order));
    }

    /**
     * 为订单追加商品明细。
     * @param ctx Javalin 请求上下文。
     */
    public void addLine(Context ctx) {
        AddLineRequest request = ctx.bodyAsClass(AddLineRequest.class);
        Order order = applicationService.addLine(new AddOrderLineCommand(
                ctx.pathParam("orderId"),
                request.productId(),
                request.productName(),
                request.quantity(),
                request.unitPrice()
        ));
        ctx.json(OrderResponse.from(order));
    }

    /**
     * 支付订单。
     * @param ctx Javalin 请求上下文。
     */
    public void pay(Context ctx) {
        ctx.json(OrderResponse.from(applicationService.pay(ctx.pathParam("orderId"))));
    }

    /**
     * 按订单号查询订单详情。
     * @param ctx Javalin 请求上下文。
     */
    public void findByOrderNo(Context ctx) {
        ctx.json(repository.findByOrderNo(ctx.pathParam("orderNo"))
                .map(OrderResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("order not found")));
    }

    /**
     * 创建订单请求参数。
     * @param orderNo 订单编号。
     * @param buyerId 买方标识。
     * @param buyerName 买方名称。
     * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
     * @since 7.1.x
     */
    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }

    /**
     * 订单追加明细请求参数。
     * @param productId 商品标识。
     * @param productName 商品名称。
     * @param quantity 数量。
     * @param unitPrice 单价。
     * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
     * @since 7.1.x
     */
    public record AddLineRequest(String productId, String productName, int quantity, BigDecimal unitPrice) {
    }

    /**
     * 订单查询响应结构。
     * @param id 订单标识。
     * @param orderNo 订单编号。
     * @param buyerId 买方标识。
     * @param buyerName 买方名称。
     * @param status 订单状态。
     * @param totalAmount 订单总金额。
     * @param lines 订单明细行集合。
     * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
     * @since 7.1.x
     */
    public record OrderResponse(
            String id,
            String orderNo,
            String buyerId,
            String buyerName,
            String status,
            BigDecimal totalAmount,
            List<OrderLineResponse> lines
    ) {

        static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.id(),
                    order.orderNo(),
                    order.buyerId(),
                    order.buyerName(),
                    order.status().name(),
                    order.totalAmount().amount(),
                    order.lines().stream().map(OrderLineResponse::from).toList()
            );
        }
    }

    /**
     * 订单明细行响应结构。
     * @param id 明细行标识。
     * @param productId 商品标识。
     * @param productName 商品名称。
     * @param quantity 数量。
     * @param subtotal 行小计金额。
     * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
     * @since 7.1.x
     */
    public record OrderLineResponse(String id, String productId, String productName, int quantity, BigDecimal subtotal) {

        static OrderLineResponse from(io.ddd4j.sample.richmodel.order.domain.model.OrderLine line) {
            return new OrderLineResponse(
                    line.id(),
                    line.productId(),
                    line.productName(),
                    line.quantity(),
                    line.subtotal().amount()
            );
        }
    }
}
