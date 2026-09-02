package io.ddd4j.mq;

import io.ddd4j.kit.lang.StrKit;

import java.util.Locale;

/**
 * 支持的 Broker 类型枚举（与 {@code ddd4j.mq.broker} 对齐）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum BrokerType {

    NONE,
    /**
     * 进程内 LMAX Disruptor 本地队列（非分布式 MQ）。
     */
    DISRUPTOR,
    RABBIT,
    KAFKA,
    ROCKET,
    PULSAR,
    REDIS_STREAM,
    ACTIVEMQ,
    NATS,
    /**
     * Eclipse Paho MQTT 客户端（连接外部 Broker，非嵌入式服务端）。
     */
    MQTT,
    /**
     * mica-mqtt AIO 客户端（sample mqtt-client2，连接外部 Broker）。
     */
    MQTT_MICA,
    ONS,
    TDMQ,
    SQS;

    /**
     * 解析配置字符串为 Broker 类型（兼容 redisStream 等历史命名）。
     */
    public static BrokerType from(String raw) {
        if (!StrKit.hasText(raw) || "none".equalsIgnoreCase(raw.trim())) {
            return NONE;
        }
        String normalized = raw.trim()
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "disruptor", "local", "local-disruptor" -> DISRUPTOR;
            case "rabbit" -> RABBIT;
            case "kafka" -> KAFKA;
            case "rocket" -> ROCKET;
            case "pulsar" -> PULSAR;
            case "redis", "redis-stream", "redisstream" -> REDIS_STREAM;
            case "activemq", "artemis" -> ACTIVEMQ;
            case "nats" -> NATS;
            case "mqtt" -> MQTT;
            case "mqtt-mica", "mica-mqtt", "mica" -> MQTT_MICA;
            case "ons" -> ONS;
            case "tdmq" -> TDMQ;
            case "sqs" -> SQS;
            default -> NONE;
        };
    }


    /**
     * 转为 kebab-case 配置值（如 {@code redis-stream}）。
     */
    public String toConfigValue() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
