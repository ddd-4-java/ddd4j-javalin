package io.ddd4j.sample.javalin.mq.kafka.mq.config;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.registry.MQListenerDefinition;
import io.ddd4j.sample.javalin.mq.kafka.order.domain.OrderCreatedEvent;

import java.lang.reflect.Method;

/**
 * Kafka MQ 配置：初始化 Kafka 生产者/消费者基础设施。
 *
 * <p>本示例演示配置结构。实际使用时需要：
 * <ul>
 *   <li>运行中的 Kafka Broker（默认 {@code localhost:9092}）</li>
 *   <li>通过 {@code ddd4j-mq-kafka} 的自动配置创建 KafkaMQEventPublisher</li>
 *   <li>消费者通过 KafkaConsumerGroup 自动管理 offset</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class KafkaMqConfig {

    private final String bootstrapServers;
    private final String namespace;

    /**
     * 构造 Kafka MQ 配置。
     * bootstrap-servers 可通过环境变量 DDD4J_MQ_KAFKA_BOOTSTRAP_SERVERS 覆盖。
     */
    public KafkaMqConfig() {
        this.bootstrapServers = System.getenv().getOrDefault(
                "DDD4J_MQ_KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        this.namespace = "javalin-kafka-sample";
    }

    /**
     * 返回 Kafka MQ 事件发布者。
     *
     * <p>生产环境应使用 ddd4j-mq-kafka 提供的 KafkaMQEventPublisher 实现。
     * 本示例为演示结构，返回简化实现。
     */
    public io.ddd4j.core.event.MQEventPublisher mqEventPublisher() {
        // 使用 ddd4j-mq-kafka 的 KafkaMQEventPublisher
        // 实际生产中由 ddd4j-mq-kafka 自动配置提供
        return new io.ddd4j.mq.publish.MQEventPublisher() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends io.ddd4j.core.event.MQEvent> void publish(T event, io.ddd4j.mq.contract.MQDestination destination) {
                System.out.printf("[Kafka Publisher] 发送事件到 Kafka: topic=%s, tag=%s, msgId=%s%n",
                        event.getTopic(), event.getTag(), event.getMsgId());
                System.out.printf("  Kafka Broker: %s%n", bootstrapServers);
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
     *
     * <p>在 Javalin 环境中手动注册；生产环境由 ddd4j-mq-kafka 自动扫描注册。
     *
     * @param listener 监听器实例
     */
    public void registerListener(Object listener) {
        try {
            Method method = listener.getClass().getMethod("onOrderCreated", OrderCreatedEvent.class);
            MQEventListener ann = method.getAnnotation(MQEventListener.class);
            if (ann != null) {
                MQListenerDefinition def = MQListenerDefinition.from(listener, method, ann);
                System.out.printf("[Kafka MQ] 注册消费者: topic=%s, tags=%s, group=%s%n",
                        def.getTopic(), def.getTags(), def.getGroup());
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("注册 MQ 监听器失败", e);
        }
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }
}
