package io.ddd4j.javalin.sample.richmodel.order;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

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
    public final static class CreateOrderRequest {

        private static final long serialVersionUID = 0L;

        private final String orderNo;

        private final String buyerId;

        private final String buyerName;

        @JsonCreator()
        public CreateOrderRequest(@JsonProperty("orderNo") String orderNo, @JsonProperty("buyerId") String buyerId, @JsonProperty("buyerName") String buyerName) {
            this.orderNo = orderNo;
            this.buyerId = buyerId;
            this.buyerName = buyerName;
        }

        @JsonProperty("orderNo")
        public String orderNo() {
            return orderNo;
        }

        @JsonProperty("buyerId")
        public String buyerId() {
            return buyerId;
        }

        @JsonProperty("buyerName")
        public String buyerName() {
            return buyerName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            CreateOrderRequest other = (CreateOrderRequest) obj;
            return Objects.equals(this.orderNo, other.orderNo) && Objects.equals(this.buyerId, other.buyerId) && Objects.equals(this.buyerName, other.buyerName);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(orderNo);
            result = 31 * result + Objects.hashCode(buyerId);
            result = 31 * result + Objects.hashCode(buyerName);
            return result;
        }

        @Override
        public String toString() {
            return "CreateOrderRequest[orderNo=" + orderNo + ", buyerId=" + buyerId + ", buyerName=" + buyerName + "]";
        }
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
    public final static class AddLineRequest {

        private static final long serialVersionUID = 0L;

        private final String productId;

        private final String productName;

        private final int quantity;

        private final BigDecimal unitPrice;

        @JsonCreator()
        public AddLineRequest(@JsonProperty("productId") String productId, @JsonProperty("productName") String productName, @JsonProperty("quantity") int quantity, @JsonProperty("unitPrice") BigDecimal unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        @JsonProperty("productId")
        public String productId() {
            return productId;
        }

        @JsonProperty("productName")
        public String productName() {
            return productName;
        }

        @JsonProperty("quantity")
        public int quantity() {
            return quantity;
        }

        @JsonProperty("unitPrice")
        public BigDecimal unitPrice() {
            return unitPrice;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            AddLineRequest other = (AddLineRequest) obj;
            return Objects.equals(this.productId, other.productId) && Objects.equals(this.productName, other.productName) && this.quantity == other.quantity && Objects.equals(this.unitPrice, other.unitPrice);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(productId);
            result = 31 * result + Objects.hashCode(productName);
            result = 31 * result + Integer.hashCode(quantity);
            result = 31 * result + Objects.hashCode(unitPrice);
            return result;
        }

        @Override
        public String toString() {
            return "AddLineRequest[productId=" + productId + ", productName=" + productName + ", quantity=" + quantity + ", unitPrice=" + unitPrice + "]";
        }
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
    public final static class OrderResponse {

        private static final long serialVersionUID = 0L;

        private final String id;

        private final String orderNo;

        private final String buyerId;

        private final String buyerName;

        private final String status;

        private final BigDecimal totalAmount;

        private final List<OrderLineResponse> lines;

        static OrderResponse from(Order order) {
            return new OrderResponse(order.id(), order.orderNo(), order.buyerId(), order.buyerName(), order.status().name(), order.totalAmount().amount(), order.lines().stream().map(OrderLineResponse::from).toList());
        }

        @JsonCreator()
        public OrderResponse(@JsonProperty("id") String id, @JsonProperty("orderNo") String orderNo, @JsonProperty("buyerId") String buyerId, @JsonProperty("buyerName") String buyerName, @JsonProperty("status") String status, @JsonProperty("totalAmount") BigDecimal totalAmount, @JsonProperty("lines") List<OrderLineResponse> lines) {
            this.id = id;
            this.orderNo = orderNo;
            this.buyerId = buyerId;
            this.buyerName = buyerName;
            this.status = status;
            this.totalAmount = totalAmount;
            this.lines = lines;
        }

        @JsonProperty("id")
        public String id() {
            return id;
        }

        @JsonProperty("orderNo")
        public String orderNo() {
            return orderNo;
        }

        @JsonProperty("buyerId")
        public String buyerId() {
            return buyerId;
        }

        @JsonProperty("buyerName")
        public String buyerName() {
            return buyerName;
        }

        @JsonProperty("status")
        public String status() {
            return status;
        }

        @JsonProperty("totalAmount")
        public BigDecimal totalAmount() {
            return totalAmount;
        }

        @JsonProperty("lines")
        public List<OrderLineResponse> lines() {
            return lines;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            OrderResponse other = (OrderResponse) obj;
            return Objects.equals(this.id, other.id) && Objects.equals(this.orderNo, other.orderNo) && Objects.equals(this.buyerId, other.buyerId) && Objects.equals(this.buyerName, other.buyerName) && Objects.equals(this.status, other.status) && Objects.equals(this.totalAmount, other.totalAmount) && Objects.equals(this.lines, other.lines);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(id);
            result = 31 * result + Objects.hashCode(orderNo);
            result = 31 * result + Objects.hashCode(buyerId);
            result = 31 * result + Objects.hashCode(buyerName);
            result = 31 * result + Objects.hashCode(status);
            result = 31 * result + Objects.hashCode(totalAmount);
            result = 31 * result + Objects.hashCode(lines);
            return result;
        }

        @Override
        public String toString() {
            return "OrderResponse[id=" + id + ", orderNo=" + orderNo + ", buyerId=" + buyerId + ", buyerName=" + buyerName + ", status=" + status + ", totalAmount=" + totalAmount + ", lines=" + lines + "]";
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
    public final static class OrderLineResponse {

        private static final long serialVersionUID = 0L;

        private final String id;

        private final String productId;

        private final String productName;

        private final int quantity;

        private final BigDecimal subtotal;

        static OrderLineResponse from(io.ddd4j.sample.richmodel.order.domain.model.OrderLine line) {
            return new OrderLineResponse(line.id(), line.productId(), line.productName(), line.quantity(), line.subtotal().amount());
        }

        @JsonCreator()
        public OrderLineResponse(@JsonProperty("id") String id, @JsonProperty("productId") String productId, @JsonProperty("productName") String productName, @JsonProperty("quantity") int quantity, @JsonProperty("subtotal") BigDecimal subtotal) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        @JsonProperty("id")
        public String id() {
            return id;
        }

        @JsonProperty("productId")
        public String productId() {
            return productId;
        }

        @JsonProperty("productName")
        public String productName() {
            return productName;
        }

        @JsonProperty("quantity")
        public int quantity() {
            return quantity;
        }

        @JsonProperty("subtotal")
        public BigDecimal subtotal() {
            return subtotal;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            OrderLineResponse other = (OrderLineResponse) obj;
            return Objects.equals(this.id, other.id) && Objects.equals(this.productId, other.productId) && Objects.equals(this.productName, other.productName) && this.quantity == other.quantity && Objects.equals(this.subtotal, other.subtotal);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(id);
            result = 31 * result + Objects.hashCode(productId);
            result = 31 * result + Objects.hashCode(productName);
            result = 31 * result + Integer.hashCode(quantity);
            result = 31 * result + Objects.hashCode(subtotal);
            return result;
        }

        @Override
        public String toString() {
            return "OrderLineResponse[id=" + id + ", productId=" + productId + ", productName=" + productName + ", quantity=" + quantity + ", subtotal=" + subtotal + "]";
        }
    }
}
