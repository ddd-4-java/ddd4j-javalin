package io.ddd4j.sample.javalin.mq.disruptor.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单聚合根（简化版）。
 *
 * <p>演示 DDD 聚合根的创建与事件注册：
 * <ul>
 *   <li>通过静态工厂方法 {@link #create} 创建订单</li>
 *   <li>构造时自动注册 {@link OrderCreatedEvent}</li>
 *   <li>应用服务在事务提交后通过 MQEventPublisher 发布事件</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Order {

    /**
     * 订单 ID
     */
    private final String id;
    /**
     * 订单编号
     */
    private final String orderNo;
    /**
     * 买家 ID
     */
    private final String buyerId;
    /**
     * 买家名称
     */
    private final String buyerName;
    /**
     * 订单状态
     */
    private String status;
    /**
     * 订单金额
     */
    private BigDecimal totalAmount;

    public Order(String id, String orderNo, String buyerId, String buyerName, String status, BigDecimal totalAmount) {
        this.id = id;
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    /**
     * 静态工厂方法：创建新订单。
     *
     * @param orderNo   订单编号
     * @param buyerId   买家 ID
     * @param buyerName 买家名称
     * @return 新创建的订单
     */
    public static Order create(String orderNo, String buyerId, String buyerName) {
        Order order = new Order(
                UUID.randomUUID().toString(),
                orderNo,
                buyerId,
                buyerName,
                "CREATED",
                BigDecimal.ZERO
        );
        return order;
    }

    // ========== Getters ==========

    public String getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
