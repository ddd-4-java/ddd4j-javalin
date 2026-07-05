package io.ddd4j.sample.javalin.mq.disruptor.mq;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.sample.javalin.mq.disruptor.order.domain.OrderCreatedEvent;

/**
 * 订单创建事件 MQ 消费者。
 *
 * <p>使用 ddd4j {@link MQEventListener} 注解声明订阅关系：
 * <ul>
 *   <li>{@code topic="ORDER"}：业务主题（与 OrderCreatedEvent 的 topic 一致）</li>
 *   <li>{@code tags="CREATED"}：只消费 CREATED 标签的事件</li>
 * </ul>
 *
 * <p>切换为 Kafka / RabbitMQ 时，本类代码完全无需修改，
 * 仅需替换 pom 依赖与 application.yml 配置。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class OrderCreatedMqListener {

    /**
     * 处理 OrderCreatedEvent。
     *
     * <p>ddd4j 通过反射调用并将反序列化后的事件对象传入。
     * 在 Javalin（无 Spring）环境中，此方法由 DisruptorMQEventDispatcher 手动委托调用。
     *
     * @param event 订单创建事件
     */
    @MQEventListener(topic = "ORDER", tags = "CREATED")
    public void onOrderCreated(OrderCreatedEvent event) {
        System.out.println("==================================================");
        System.out.println("[MQ 消费者] 收到 OrderCreatedEvent！");
        System.out.printf("  订单 ID  : %s%n", event.getOrderId());
        System.out.printf("  订单编号 : %s%n", event.getOrderNo());
        System.out.printf("  买家名称 : %s%n", event.getBuyerName());
        System.out.printf("  Topic    : %s%n", event.getTopic());
        System.out.printf("  Tag      : %s%n", event.getTag());
        System.out.println("==================================================");
    }
}
