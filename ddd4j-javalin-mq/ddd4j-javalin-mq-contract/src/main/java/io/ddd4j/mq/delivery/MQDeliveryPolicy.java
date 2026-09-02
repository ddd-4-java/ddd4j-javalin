package io.ddd4j.mq.delivery;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 可靠消息的租约和退避策略。
 *
 * <p>默认值采用 60 秒租约、12 次最多尝试，以及 1 秒到 5 分钟的指数退避。
 */
public record MQDeliveryPolicy(
        Duration leaseDuration,
        int maxAttempts,
        Duration initialBackoff,
        Duration maxBackoff,
        double jitterFactor) {

    private static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(60);
    private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration DEFAULT_MAX_BACKOFF = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_ATTEMPTS = 12;
    private static final double DEFAULT_JITTER_FACTOR = 0.20D;

    public MQDeliveryPolicy {
        Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        Objects.requireNonNull(initialBackoff, "initialBackoff must not be null");
        Objects.requireNonNull(maxBackoff, "maxBackoff must not be null");
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
        if (initialBackoff.isZero() || initialBackoff.isNegative()) {
            throw new IllegalArgumentException("initialBackoff must be positive");
        }
        if (maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("maxBackoff must not be less than initialBackoff");
        }
        if (jitterFactor < 0.0D || jitterFactor > 1.0D) {
            throw new IllegalArgumentException("jitterFactor must be between zero and one");
        }
    }

    /**
     * 返回生产环境默认投递策略。
     *
     * @return 默认策略
     */
    public static MQDeliveryPolicy productionDefault() {
        return new MQDeliveryPolicy(DEFAULT_LEASE_DURATION, DEFAULT_MAX_ATTEMPTS,
                DEFAULT_INITIAL_BACKOFF, DEFAULT_MAX_BACKOFF, DEFAULT_JITTER_FACTOR);
    }

    /**
     * 计算某次失败后的下一次可投递时间。
     *
     * @param attempts 已完成的发送尝试次数，从 1 开始
     * @param failedAt 失败发生时间
     * @param randomUnitInterval [0, 1] 的随机值，由调用方提供以保持测试可重复
     * @return 带抖动的下一次可投递时间
     */
    public Instant nextAvailableAt(int attempts, Instant failedAt, double randomUnitInterval) {
        Objects.requireNonNull(failedAt, "failedAt must not be null");
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must be greater than zero");
        }
        if (randomUnitInterval < 0.0D || randomUnitInterval > 1.0D) {
            throw new IllegalArgumentException("randomUnitInterval must be between zero and one");
        }
        long baseMillis = exponentialBackoffMillis(attempts);
        double jitter = 1.0D + ((randomUnitInterval * 2.0D - 1.0D) * jitterFactor);
        return failedAt.plusMillis(Math.round(baseMillis * jitter));
    }

    /**
     * 判断某条消息是否应进入死信状态。
     *
     * @param attempts 已完成的发送尝试次数
     * @return 是否耗尽重试次数
     */
    public boolean exhausted(int attempts) {
        return attempts >= maxAttempts;
    }

    private long exponentialBackoffMillis(int attempts) {
        long maxMillis = maxBackoff.toMillis();
        long initialMillis = initialBackoff.toMillis();
        int exponent = Math.min(attempts - 1, 62);
        long multiplier = 1L << exponent;
        if (initialMillis > maxMillis / multiplier) {
            return maxMillis;
        }
        return Math.min(initialMillis * multiplier, maxMillis);
    }
}
