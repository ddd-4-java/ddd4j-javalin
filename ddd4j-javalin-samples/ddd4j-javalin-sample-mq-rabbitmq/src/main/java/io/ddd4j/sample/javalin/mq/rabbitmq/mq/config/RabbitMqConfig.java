package io.ddd4j.sample.javalin.mq.rabbitmq.mq.config;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.registry.MQListenerDefinition;
import io.ddd4j.sample.javalin.mq.rabbitmq.order.domain.OrderCreatedEvent;

import java.lang.reflect.Method;

/**
 * RabbitMQ (AMQP) 配置：初始化 RabbitMQ 连接与消费者。
 *
 * <p>RabbitMQ 特性：
 * <ul>
 *   <li>Exchange（直连/主题/扇出/头部）路由消息到 Queue</li>
 *   <li>消息确认（ACK/NACK/Reject）保证可靠消费</li>
 *   <li>死信队列（DLX）处理消费失败的消息</li>
 *   <li>支持优先级队列、延迟队列等高级特性</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class RabbitMqConfig {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String virtualHost;

    /**
     * 构造 RabbitMQ 配置。
     * 连接参数可通过环境变量覆盖：
     * <ul>
     *   <li>{@code DDD4J_MQ_RABBITMQ_HOST}（默认 localhost）</li>
     *   <li>{@code DDD4J_MQ_RABBITMQ_PORT}（默认 5672）</li>
     *   <li>{@code DDD4J_MQ_RABBITMQ_USERNAME}（默认 guest）</li>
     *   <li>{@code DDD4J_MQ_RABBITMQ_PASSWORD}（默认 guest）</li>
     * </ul>
     */
    public RabbitMqConfig() {
        this.host = System.getenv().getOrDefault("DDD4J_MQ_RABBITMQ_HOST", "localhost");
        this.port = Integer.parseInt(System.getenv().getOrDefault("DDD4J_MQ_RABBITMQ_PORT", "5672"));
        this.username = System.getenv().getOrDefault("DDD4J_MQ_RABBITMQ_USERNAME", "guest");
        this.password = System.getenv().getOrDefault("DDD4J_MQ_RABBITMQ_PASSWORD", "guest");
        this.virtualHost = "/";
    }

    /**
     * 返回 RabbitMQ 事件发布者。
     *
     * <p>生产环境应使用 ddd4j-mq-rabbitmq 提供的 RabbitMQEventPublisher 实现。
     * 本示例为演示结构，返回简化实现。
     */
    public io.ddd4j.core.event.MQEventPublisher mqEventPublisher() {
        return new io.ddd4j.mq.publish.MQEventPublisher() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends io.ddd4j.core.event.MQEvent> void publish(T event, io.ddd4j.mq.contract.MQDestination destination) {
                System.out.printf("[RabbitMQ Publisher] 发送事件到 RabbitMQ: exchange=ORDER_EXCHANGE, routingKey=%s.%s%n",
                        event.getTopic(), event.getTag());
                System.out.printf("  RabbitMQ: amqp://%s:%d%s%n", host, port, virtualHost);
                System.out.printf("  载荷: %s%n", io.ddd4j.kit.lang.JsonKit.toJson(event));
            }

            @Override
            public void publish(io.ddd4j.core.event.MQEvent event) {
                publish(event, io.ddd4j.mq.contract.MQDestination.from(event));
            }
        };
    }

    /**
     * 注册 MQ 消费者监听器。
     */
    public void registerListener(Object listener) {
        try {
            Method method = listener.getClass().getMethod("onOrderCreated", OrderCreatedEvent.class);
            MQEventListener ann = method.getAnnotation(MQEventListener.class);
            if (ann != null) {
                MQListenerDefinition def = MQListenerDefinition.from(listener, method, ann);
                System.out.printf("[RabbitMQ] 注册消费者: topic=%s, tags=%s, group=%s%n",
                        def.getTopic(), def.getTags(), def.getGroup());
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("注册 MQ 监听器失败", e);
        }
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
}
