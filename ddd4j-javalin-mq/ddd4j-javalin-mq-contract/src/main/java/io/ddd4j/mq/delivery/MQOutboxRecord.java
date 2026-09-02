package io.ddd4j.mq.delivery;

import io.ddd4j.kit.lang.StrKit;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 可持久化的 Outbox 消息快照。
 *
 * <p>该对象不绑定 JSON、数据库或 broker；存储适配器负责将其映射为自己的表结构。
 */
public record MQOutboxRecord(
        String messageId,
        String destination,
        String payload,
        Map<String, String> headers,
        MQOutboxStatus status,
        Instant availableAt,
        String leaseOwner,
        Instant leaseUntil,
        int attempts,
        String lastError,
        Instant publishedAt) {

    public MQOutboxRecord {
        if (StrKit.isBlank(messageId)) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        if (StrKit.isBlank(destination)) {
            throw new IllegalArgumentException("destination must not be blank");
        }
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(availableAt, "availableAt must not be null");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        Map<String, String> mutableHeaders = new LinkedHashMap<>(headers);
        mutableHeaders.put(MQDeliveryHeaders.MESSAGE_ID, messageId);
        headers = Map.copyOf(mutableHeaders);
    }

    /**
     * 创建一条等待发布的消息。
     *
     * @param messageId 稳定消息标识
     * @param destination broker 目的地
     * @param payload 已序列化事件负载
     * @param headers 业务消息头
     * @param availableAt 首次可投递时间
     * @return 待发布记录
     */
    public static MQOutboxRecord pending(String messageId, String destination, String payload,
                                         Map<String, String> headers, Instant availableAt) {
        return new MQOutboxRecord(messageId, destination, payload, headers, MQOutboxStatus.PENDING,
                availableAt, null, null, 0, null, null);
    }
}
