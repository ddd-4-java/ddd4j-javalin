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
     * 构造订单路由适配器。
     *
     * @param applicationService  订单应用服务
     * @param repository          订单仓储
     */
    @Inject
    public JavalinOrderController(OrderApplicationService applicationService, OrderRepository repository) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * 用 Javalin 编程式 API 注册订单路由。
     *
     * @param app  Javalin 应用实例
     */
    public void register(Javalin app) {
        app.post("/orders", this::create);
        app.post("/orders/{orderId}/lines", this::addLine);
        app.post("/orders/{orderId}/pay", this::pay);
        app.get("/orders/by-no/{orderNo}", this::findByOrderNo);
    }

    /**
     * 创建订单草稿：POST /orders。
     *
     * @param ctx  Javalin 请求上下文
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
     * 添加订单行：POST /orders/{orderId}/lines。
     *
     * @param ctx  Javalin 请求上下文
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
     * 支付订单：POST /orders/{orderId}/pay。
     *
     * @param ctx  Javalin 请求上下文
     */
    public void pay(Context ctx) {
        ctx.json(OrderResponse.from(applicationService.pay(ctx.pathParam("orderId"))));
    }

    /**
     * 按订单号查询订单：GET /orders/by-no/{orderNo}。
     *
     * @param ctx  Javalin 请求上下文
     */
    public void findByOrderNo(Context ctx) {
        ctx.json(repository.findByOrderNo(ctx.pathParam("orderNo"))
                .map(OrderResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("order not found")));
    }

    /**
     * 创建订单请求载荷。
     */
    public final static class CreateOrderRequest {

        private static final long serialVersionUID = 0L;

        private final String orderNo;

        private final String buyerId;

        private final String buyerName;

        /**
         * 构造创建订单请求。
         *
         * @param orderNo    订单号
         * @param buyerId    买家 ID
         * @param buyerName  买家名称
         */
        @JsonCreator()
        public CreateOrderRequest(@JsonProperty("orderNo") String orderNo, @JsonProperty("buyerId") String buyerId, @JsonProperty("buyerName") String buyerName) {
            this.orderNo = orderNo;
            this.buyerId = buyerId;
            this.buyerName = buyerName;
        }

        /**
         * 返回订单号。
         *
         * @return 订单号
         */
        @JsonProperty("orderNo")
        public String orderNo() {
            return orderNo;
        }

        /**
         * 返回买家 ID。
         *
         * @return 买家 ID
         */
        @JsonProperty("buyerId")
        public String buyerId() {
            return buyerId;
        }

        /**
         * 返回买家名称。
         *
         * @return 买家名称
         */
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
     * 添加订单行请求载荷。
     */
    public final static class AddLineRequest {

        private static final long serialVersionUID = 0L;

        private final String productId;

        private final String productName;

        private final int quantity;

        private final BigDecimal unitPrice;

        /**
         * 构造添加订单行请求。
         *
         * @param productId    商品 ID
         * @param productName  商品名称
         * @param quantity     数量
         * @param unitPrice    单价
         */
        @JsonCreator()
        public AddLineRequest(@JsonProperty("productId") String productId, @JsonProperty("productName") String productName, @JsonProperty("quantity") int quantity, @JsonProperty("unitPrice") BigDecimal unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        /**
         * 返回商品 ID。
         *
         * @return 商品 ID
         */
        @JsonProperty("productId")
        public String productId() {
            return productId;
        }

        /**
         * 返回商品名称。
         *
         * @return 商品名称
         */
        @JsonProperty("productName")
        public String productName() {
            return productName;
        }

        /**
         * 返回数量。
         *
         * @return 数量
         */
        @JsonProperty("quantity")
        public int quantity() {
            return quantity;
        }

        /**
         * 返回单价。
         *
         * @return 单价
         */
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
     * 订单响应投影（读模型）。
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

        /**
         * 由领域订单投影为响应模型。
         *
         * @param order  领域订单
         * @return 订单响应
         */
        static OrderResponse from(Order order) {
            return new OrderResponse(order.id(), order.orderNo(), order.buyerId(), order.buyerName(), order.status().name(), order.totalAmount().amount(), order.lines().stream().map(OrderLineResponse::from).toList());
        }

        /**
         * 构造订单响应投影。
         *
         * @param id           订单 ID
         * @param orderNo      订单号
         * @param buyerId      买家 ID
         * @param buyerName    买家名称
         * @param status       订单状态
         * @param totalAmount  订单总额
         * @param lines        订单行列表
         */
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

        /**
         * 返回订单 ID。
         *
         * @return 订单 ID
         */
        @JsonProperty("id")
        public String id() {
            return id;
        }

        /**
         * 返回订单号。
         *
         * @return 订单号
         */
        @JsonProperty("orderNo")
        public String orderNo() {
            return orderNo;
        }

        /**
         * 返回买家 ID。
         *
         * @return 买家 ID
         */
        @JsonProperty("buyerId")
        public String buyerId() {
            return buyerId;
        }

        /**
         * 返回买家名称。
         *
         * @return 买家名称
         */
        @JsonProperty("buyerName")
        public String buyerName() {
            return buyerName;
        }

        /**
         * 返回订单状态。
         *
         * @return 订单状态
         */
        @JsonProperty("status")
        public String status() {
            return status;
        }

        /**
         * 返回订单总额。
         *
         * @return 订单总额
         */
        @JsonProperty("totalAmount")
        public BigDecimal totalAmount() {
            return totalAmount;
        }

        /**
         * 返回订单行列表。
         *
         * @return 订单行列表
         */
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
     * 订单行响应投影（读模型）。
     */
    public final static class OrderLineResponse {

        private static final long serialVersionUID = 0L;

        private final String id;

        private final String productId;

        private final String productName;

        private final int quantity;

        private final BigDecimal subtotal;

        /**
         * 由领域订单行投影为响应模型。
         *
         * @param line  领域订单行
         * @return 订单行响应
         */
        static OrderLineResponse from(io.ddd4j.sample.richmodel.order.domain.model.OrderLine line) {
            return new OrderLineResponse(line.id(), line.productId(), line.productName(), line.quantity(), line.subtotal().amount());
        }

        /**
         * 构造订单行响应投影。
         *
         * @param id           订单行 ID
         * @param productId    商品 ID
         * @param productName  商品名称
         * @param quantity     数量
         * @param subtotal     小计
         */
        @JsonCreator()
        public OrderLineResponse(@JsonProperty("id") String id, @JsonProperty("productId") String productId, @JsonProperty("productName") String productName, @JsonProperty("quantity") int quantity, @JsonProperty("subtotal") BigDecimal subtotal) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        /**
         * 返回订单行 ID。
         *
         * @return 订单行 ID
         */
        @JsonProperty("id")
        public String id() {
            return id;
        }

        /**
         * 返回商品 ID。
         *
         * @return 商品 ID
         */
        @JsonProperty("productId")
        public String productId() {
            return productId;
        }

        /**
         * 返回商品名称。
         *
         * @return 商品名称
         */
        @JsonProperty("productName")
        public String productName() {
            return productName;
        }

        /**
         * 返回数量。
         *
         * @return 数量
         */
        @JsonProperty("quantity")
        public int quantity() {
            return quantity;
        }

        /**
         * 返回小计。
         *
         * @return 小计
         */
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
