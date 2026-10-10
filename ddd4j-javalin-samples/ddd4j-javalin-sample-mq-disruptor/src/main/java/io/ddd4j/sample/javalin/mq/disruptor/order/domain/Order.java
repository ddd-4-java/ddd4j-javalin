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

    /**
     * 返回订单标识。
     * @return 订单标识。
     */
    public String getId() {
        return id;
    }

    /**
     * 返回订单号。
     * @return 方法执行结果。
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 返回买家标识。
     * @return 方法执行结果。
     */
    public String getBuyerId() {
        return buyerId;
    }

    /**
     * 返回买家名称。
     * @return 方法执行结果。
     */
    public String getBuyerName() {
        return buyerName;
    }

    /**
     * 返回订单状态。
     * @return 订单状态。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 返回订单总金额。
     * @return 方法执行结果。
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
