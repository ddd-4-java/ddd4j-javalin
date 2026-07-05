package io.ddd4j.sample.javalin.mq.disruptor.order.application;

import io.ddd4j.core.event.MQEventPublisher;
import io.ddd4j.sample.javalin.mq.disruptor.order.domain.Order;
import io.ddd4j.sample.javalin.mq.disruptor.order.domain.OrderCreatedEvent;

/**
 * 订单应用服务：编排创建订单用例并发布领域事件。
 *
 * <p>核心流程：
 * <ol>
 *   <li>创建 Order 聚合</li>
 *   <li>构建 OrderCreatedEvent</li>
 *   <li>通过 MQEventPublisher.publish() 投递到 Disruptor RingBuffer</li>
 *   <li>RingBuffer → DisruptorMQEventDispatcher → @MQEventListener 消费</li>
 * </ol>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class OrderApplicationService {

    private final MQEventPublisher mqEventPublisher;

    /**
     * 构造函数。
     *
     * @param mqEventPublisher MQ 事件发布者（由 DisruptorMqConfig 创建，已注入 BaseContext）
     */
    public OrderApplicationService(MQEventPublisher mqEventPublisher) {
        this.mqEventPublisher = mqEventPublisher;
    }

    /**
     * 创建订单并发布 OrderCreatedEvent。
     *
     * @param orderNo   订单编号
     * @param buyerId   买家 ID
     * @param buyerName 买家名称
     * @return 创建的订单
     */
    public Order createOrder(String orderNo, String buyerId, String buyerName) {
        // 1. 创建订单聚合
        Order order = Order.create(orderNo, buyerId, buyerName);

        // 2. 构建领域事件
        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getOrderNo(), order.getBuyerName());

        // 3. 发布到 MQ（Disruptor RingBuffer → @MQEventListener 消费）
        mqEventPublisher.publish(event);

        System.out.printf("[应用服务] 订单已创建: id=%s, orderNo=%s, 买家=%s%n",
                order.getId(), order.getOrderNo(), order.getBuyerName());
        return order;
    }
}
