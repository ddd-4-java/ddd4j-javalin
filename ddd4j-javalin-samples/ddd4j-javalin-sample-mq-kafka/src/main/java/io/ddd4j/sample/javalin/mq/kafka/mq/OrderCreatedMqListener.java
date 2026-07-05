package io.ddd4j.sample.javalin.mq.kafka.mq;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.sample.javalin.mq.kafka.order.domain.OrderCreatedEvent;

/**
 * 订单创建事件 Kafka MQ 消费者。
 *
 * <p>使用 {@link MQEventListener} 声明订阅关系，与 Disruptor 示例的监听器代码完全一致。
 * 切换 MQ 时本类无需修改。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class OrderCreatedMqListener {

    /**
     * 处理 OrderCreatedEvent（从 Kafka Topic 消费）。
     *
     * @param event 订单创建事件
     */
    @MQEventListener(topic = "ORDER", tags = "CREATED")
    public void onOrderCreated(OrderCreatedEvent event) {
        System.out.println("==================================================");
        System.out.println("[Kafka MQ 消费者] 收到 OrderCreatedEvent！");
        System.out.printf("  订单 ID  : %s%n", event.getOrderId());
        System.out.printf("  订单编号 : %s%n", event.getOrderNo());
        System.out.printf("  买家名称 : %s%n", event.getBuyerName());
        System.out.printf("  Topic    : %s%n", event.getTopic());
        System.out.printf("  Tag      : %s%n", event.getTag());
        System.out.println("==================================================");
    }
}
