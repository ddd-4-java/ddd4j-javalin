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

    @Inject
    public JavalinOrderController(OrderApplicationService applicationService, OrderRepository repository) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public void register(Javalin app) {
        app.unsafe.routes.post("/orders", this::create);
        app.unsafe.routes.post("/orders/{orderId}/lines", this::addLine);
        app.unsafe.routes.post("/orders/{orderId}/pay", this::pay);
        app.unsafe.routes.get("/orders/by-no/{orderNo}", this::findByOrderNo);
    }

    public void create(Context ctx) {
        CreateOrderRequest request = ctx.bodyAsClass(CreateOrderRequest.class);
        Order order = applicationService.createDraft(new CreateOrderCommand(
                request.orderNo(),
                request.buyerId(),
                request.buyerName()
        ));
        ctx.json(OrderResponse.from(order));
    }

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

    public void pay(Context ctx) {
        ctx.json(OrderResponse.from(applicationService.pay(ctx.pathParam("orderId"))));
    }

    public void findByOrderNo(Context ctx) {
        ctx.json(repository.findByOrderNo(ctx.pathParam("orderNo"))
                .map(OrderResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("order not found")));
    }

    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }

    public record AddLineRequest(String productId, String productName, int quantity, BigDecimal unitPrice) {
    }

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
