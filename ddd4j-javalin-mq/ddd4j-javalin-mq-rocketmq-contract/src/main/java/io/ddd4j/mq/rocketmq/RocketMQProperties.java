package io.ddd4j.mq.rocketmq;

import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

/**
 * RocketMQ adapter configuration.
 *
 * <p>{@link RocketMQProperties} extends {@link MQProperties} —— 复用通用字段（namespace / defaultTopic /
 * autoAck / persist / retries / username / password / producerGroup 等），仅声明 RocketMQ 专属字段。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RocketMQProperties extends MQProperties {

    /**
     * NameServer 地址（例：{@code localhost:9876} 或 {@code 192.168.1.1:9876;192.168.1.2:9876}）。
     */
    private String nameServer = "localhost:9876";
    /**
     * 消费者组名前缀（每个 listener 的 group 回落到 {@code consumerGroupPrefix + method}）。
     */
    private String consumerGroupPrefix = "ddd4j";
    /**
     * 生产者是否自动启动。
     */
    private boolean autoStartProducer = true;
    /**
     * 消费者是否自动启动。
     */
    private boolean autoStartConsumers = true;

    /**
     * 基于本配置创建原生生产者（含 nameServer）。
     */
    public DefaultMQProducer newProducer() {
        DefaultMQProducer producer = new DefaultMQProducer(getProducerGroup());
        if (StrKit.isNotEmpty(nameServer)) {
            producer.setNamesrvAddr(nameServer);
        }
        return producer;
    }

    /**
     * 基于本配置创建原生消费者（含 nameServer）。
     */
    public DefaultMQPushConsumer newConsumer(String group) {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group);
        if (StrKit.isNotEmpty(nameServer)) {
            consumer.setNamesrvAddr(nameServer);
        }
        return consumer;
    }
}
