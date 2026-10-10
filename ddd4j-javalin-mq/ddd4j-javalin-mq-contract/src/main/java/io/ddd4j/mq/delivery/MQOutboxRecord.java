package io.ddd4j.mq.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

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
public final class MQOutboxRecord {

    private static final long serialVersionUID = 0L;

    private final String messageId;

    private final String destination;

    private final String payload;

    private final Map<String, String> headers;

    private final MQOutboxStatus status;

    private final Instant availableAt;

    private final String leaseOwner;

    private final Instant leaseUntil;

    private final int attempts;

    private final String lastError;

    private final Instant publishedAt;

    @JsonCreator()
    public MQOutboxRecord(@JsonProperty("messageId") String messageId, @JsonProperty("destination") String destination, @JsonProperty("payload") String payload, @JsonProperty("headers") Map<String, String> headers, @JsonProperty("status") MQOutboxStatus status, @JsonProperty("availableAt") Instant availableAt, @JsonProperty("leaseOwner") String leaseOwner, @JsonProperty("leaseUntil") Instant leaseUntil, @JsonProperty("attempts") int attempts, @JsonProperty("lastError") String lastError, @JsonProperty("publishedAt") Instant publishedAt) {
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
        this.messageId = messageId;
        this.destination = destination;
        this.payload = payload;
        this.headers = headers;
        this.status = status;
        this.availableAt = availableAt;
        this.leaseOwner = leaseOwner;
        this.leaseUntil = leaseUntil;
        this.attempts = attempts;
        this.lastError = lastError;
        this.publishedAt = publishedAt;
    }

    /**
     * 创建一条等待发布的消息。
     *
     * @param messageId   稳定消息标识
     * @param destination broker 目的地
     * @param payload     已序列化事件负载
     * @param headers     业务消息头
     * @param availableAt 首次可投递时间
     * @return 待发布记录
     */
    public static MQOutboxRecord pending(String messageId, String destination, String payload, Map<String, String> headers, Instant availableAt) {
        return new MQOutboxRecord(messageId, destination, payload, headers, MQOutboxStatus.PENDING, availableAt, null, null, 0, null, null);
    }

    @JsonProperty("messageId")
    public String messageId() {
        return messageId;
    }

    @JsonProperty("destination")
    public String destination() {
        return destination;
    }

    @JsonProperty("payload")
    public String payload() {
        return payload;
    }

    @JsonProperty("headers")
    public Map<String, String> headers() {
        return headers;
    }

    @JsonProperty("status")
    public MQOutboxStatus status() {
        return status;
    }

    @JsonProperty("availableAt")
    public Instant availableAt() {
        return availableAt;
    }

    @JsonProperty("leaseOwner")
    public String leaseOwner() {
        return leaseOwner;
    }

    @JsonProperty("leaseUntil")
    public Instant leaseUntil() {
        return leaseUntil;
    }

    @JsonProperty("attempts")
    public int attempts() {
        return attempts;
    }

    @JsonProperty("lastError")
    public String lastError() {
        return lastError;
    }

    @JsonProperty("publishedAt")
    public Instant publishedAt() {
        return publishedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        MQOutboxRecord other = (MQOutboxRecord) obj;
        return Objects.equals(this.messageId, other.messageId) && Objects.equals(this.destination, other.destination) && Objects.equals(this.payload, other.payload) && Objects.equals(this.headers, other.headers) && Objects.equals(this.status, other.status) && Objects.equals(this.availableAt, other.availableAt) && Objects.equals(this.leaseOwner, other.leaseOwner) && Objects.equals(this.leaseUntil, other.leaseUntil) && this.attempts == other.attempts && Objects.equals(this.lastError, other.lastError) && Objects.equals(this.publishedAt, other.publishedAt);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(messageId);
        result = 31 * result + Objects.hashCode(destination);
        result = 31 * result + Objects.hashCode(payload);
        result = 31 * result + Objects.hashCode(headers);
        result = 31 * result + Objects.hashCode(status);
        result = 31 * result + Objects.hashCode(availableAt);
        result = 31 * result + Objects.hashCode(leaseOwner);
        result = 31 * result + Objects.hashCode(leaseUntil);
        result = 31 * result + Integer.hashCode(attempts);
        result = 31 * result + Objects.hashCode(lastError);
        result = 31 * result + Objects.hashCode(publishedAt);
        return result;
    }

    @Override
    public String toString() {
        return "MQOutboxRecord[messageId=" + messageId + ", destination=" + destination + ", payload=" + payload + ", headers=" + headers + ", status=" + status + ", availableAt=" + availableAt + ", leaseOwner=" + leaseOwner + ", leaseUntil=" + leaseUntil + ", attempts=" + attempts + ", lastError=" + lastError + ", publishedAt=" + publishedAt + "]";
    }
}
