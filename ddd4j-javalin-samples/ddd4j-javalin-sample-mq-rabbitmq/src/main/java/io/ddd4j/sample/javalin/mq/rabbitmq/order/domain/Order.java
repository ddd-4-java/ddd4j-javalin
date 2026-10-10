package io.ddd4j.sample.javalin.mq.rabbitmq.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单聚合根（简化版）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Order {

    private final String id;
    private final String orderNo;
    private final String buyerId;
    private final String buyerName;
    private final String status;
    private final BigDecimal totalAmount;

    /**
     * 构造 Order 实例并初始化依赖。
     * @param id 标识值。
     * @param orderNo 订单号。
     * @param buyerId 买家标识。
     * @param buyerName 买家名称。
     * @param status 状态。
     * @param totalAmount 订单总金额。
     */
    public Order(String id, String orderNo, String buyerId, String buyerName, String status, BigDecimal totalAmount) {
        this.id = id;
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    /**
     * 创建订单聚合根。
     * @param orderNo 订单号。
     * @param buyerId 买家标识。
     * @param buyerName 买家名称。
     * @return 创建出的资源。
     */
    public static Order create(String orderNo, String buyerId, String buyerName) {
        return new Order(UUID.randomUUID().toString(), orderNo, buyerId, buyerName, "CREATED", BigDecimal.ZERO);
    }

    /**
     * 返回订单标识。
     * @return 订单标识。
     */
    public String getId() { return id; }
    /**
     * 返回订单号。
     * @return 方法执行结果。
     */
    public String getOrderNo() { return orderNo; }
    /**
     * 返回买家标识。
     * @return 方法执行结果。
     */
    public String getBuyerId() { return buyerId; }
    /**
     * 返回买家名称。
     * @return 方法执行结果。
     */
    public String getBuyerName() { return buyerName; }
    /**
     * 返回订单状态。
     * @return 订单状态。
     */
    public String getStatus() { return status; }
    /**
     * 返回订单总金额。
     * @return 方法执行结果。
     */
    public BigDecimal getTotalAmount() { return totalAmount; }
}
