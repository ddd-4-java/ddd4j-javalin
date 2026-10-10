package io.ddd4j.javalin.sample.orderoutbox;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

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
     */
    public final static class CreateOrderRequest {

        private static final long serialVersionUID = 0L;

        private final String orderNo;

        private final String buyerId;

        private final String buyerName;

        /**
         * 构造创建订单请求。
         *
         * @param orderNo   订单号
         * @param buyerId   买家 ID
         * @param buyerName 买家名称
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
    public final static class AddOrderLineRequest {

        private static final long serialVersionUID = 0L;

        private final String goodsId;

        private final String goodsName;

        private final int quantity;

        private final BigDecimal unitPrice;

        /**
         * 构造添加订单行请求。
         *
         * @param goodsId   商品 ID
         * @param goodsName 商品名称
         * @param quantity  数量
         * @param unitPrice 单价
         */
        @JsonCreator()
        public AddOrderLineRequest(@JsonProperty("goodsId") String goodsId, @JsonProperty("goodsName") String goodsName, @JsonProperty("quantity") int quantity, @JsonProperty("unitPrice") BigDecimal unitPrice) {
            this.goodsId = goodsId;
            this.goodsName = goodsName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        /**
         * 返回商品 ID。
         *
         * @return 商品 ID
         */
        @JsonProperty("goodsId")
        public String goodsId() {
            return goodsId;
        }

        /**
         * 返回商品名称。
         *
         * @return 商品名称
         */
        @JsonProperty("goodsName")
        public String goodsName() {
            return goodsName;
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
            AddOrderLineRequest other = (AddOrderLineRequest) obj;
            return Objects.equals(this.goodsId, other.goodsId) && Objects.equals(this.goodsName, other.goodsName) && this.quantity == other.quantity && Objects.equals(this.unitPrice, other.unitPrice);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(goodsId);
            result = 31 * result + Objects.hashCode(goodsName);
            result = 31 * result + Integer.hashCode(quantity);
            result = 31 * result + Objects.hashCode(unitPrice);
            return result;
        }

        @Override
        public String toString() {
            return "AddOrderLineRequest[goodsId=" + goodsId + ", goodsName=" + goodsName + ", quantity=" + quantity + ", unitPrice=" + unitPrice + "]";
        }
    }
}
