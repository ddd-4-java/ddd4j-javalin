package io.ddd4j.sample.javalin.mq.kafka.order.application;

import io.ddd4j.core.event.MQEventPublisher;
import io.ddd4j.sample.javalin.mq.kafka.order.domain.Order;
import io.ddd4j.sample.javalin.mq.kafka.order.domain.OrderCreatedEvent;

/**
 * 订单应用服务：编排创建订单用例并发布领域事件到 Kafka。
 *
 * <p>核心流程与 Disruptor 示例完全一致：
 * <ol>
 *   <li>创建 Order 聚合</li>
 *   <li>构建 OrderCreatedEvent</li>
 *   <li>通过 MQEventPublisher.publish() 投递到 Kafka Topic</li>
 *   <li>Kafka 消费者 → @MQEventListener 消费</li>
 * </ol>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class OrderApplicationService {

    private final MQEventPublisher mqEventPublisher;

    public OrderApplicationService(MQEventPublisher mqEventPublisher) {
        this.mqEventPublisher = mqEventPublisher;
    }

    /**
     * 创建订单并发布 OrderCreatedEvent。
     */
    public Order createOrder(String orderNo, String buyerId, String buyerName) {
        Order order = Order.create(orderNo, buyerId, buyerName);
        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getOrderNo(), order.getBuyerName());

        // 发布到 Kafka（MQEventPublisher 实现透明切换）
        mqEventPublisher.publish(event);

        System.out.printf("[应用服务] 订单已创建: id=%s, orderNo=%s, 买家=%s (已投递 Kafka)%n",
                order.getId(), order.getOrderNo(), order.getBuyerName());
        return order;
    }
}
