package io.ddd4j.mq.delivery;

import io.ddd4j.kit.lang.StrKit;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Outbox 可靠投递协调器。
 *
 * <p>领取和状态变更由 {@link MQOutboxStore} 在短事务中实现；本类在事务外调用 {@link MQOutboxSender}，
 * 因而 broker 延迟不会长期占用业务数据库锁。发送成功但条件确认失败时保留至少一次语义。
 */
@Slf4j
public final class MQOutboxDispatcher {

    private final MQOutboxStore store;
    private final MQOutboxSender sender;
    private final MQDeliveryPolicy policy;
    private final MQDeliveryObserver observer;

    public MQOutboxDispatcher(MQOutboxStore store, MQOutboxSender sender, MQDeliveryPolicy policy) {
        this(store, sender, policy, NoopMQDeliveryObserver.INSTANCE);
    }

    /**
     * 创建带投递结果观察器的调度器。
     *
     * @param store Outbox 持久化端口
     * @param sender Broker 发送端口
     * @param policy 投递策略
     * @param observer 旁路观测实现
     */
    public MQOutboxDispatcher(MQOutboxStore store, MQOutboxSender sender, MQDeliveryPolicy policy,
                              MQDeliveryObserver observer) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.observer = Objects.requireNonNull(observer, "observer must not be null");
    }

    /**
     * 调度一批当前可投递消息。
     *
     * @param leaseOwner 当前发布实例标识
     * @param limit 本轮最多处理数量
     * @param now 当前时间
     * @return 调度结果
     */
    public MQOutboxDispatchResult dispatch(String leaseOwner, int limit, Instant now) {
        if (StrKit.isBlank(leaseOwner)) {
            throw new IllegalArgumentException("leaseOwner must not be blank");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
        Objects.requireNonNull(now, "now must not be null");
        List<MQOutboxRecord> claimed = store.claim(leaseOwner, now, limit, policy);
        int published = 0;
        int rescheduled = 0;
        int dead = 0;
        int confirmationLost = 0;
        for (MQOutboxRecord record : claimed) {
            try {
                sender.send(record);
                if (store.markPublished(record.messageId(), leaseOwner, now)) {
                    published++;
                    notifyObserver(deliveryObserver -> deliveryObserver.onOutboxPublished(record));
                } else {
                    confirmationLost++;
                    log.warn("Outbox message {} was sent but its lease confirmation was lost", record.messageId());
                    notifyObserver(deliveryObserver -> deliveryObserver.onOutboxFailed(record));
                }
            } catch (RuntimeException exception) {
                try {
                    if (store.reschedule(record.messageId(), leaseOwner, now, exception.getMessage(), policy)) {
                        if (policy.exhausted(record.attempts())) {
                            dead++;
                            notifyObserver(deliveryObserver -> deliveryObserver.onOutboxDead(record));
                        } else {
                            rescheduled++;
                            notifyObserver(deliveryObserver -> deliveryObserver.onOutboxRetry(record));
                        }
                    } else {
                        notifyObserver(deliveryObserver -> deliveryObserver.onOutboxFailed(record));
                    }
                } catch (RuntimeException storeException) {
                    notifyObserver(deliveryObserver -> deliveryObserver.onOutboxFailed(record));
                    log.warn("Outbox message {} delivery state update failed", record.messageId(), storeException);
                    throw storeException;
                }
                log.warn("Outbox message {} delivery failed", record.messageId(), exception);
            }
        }
        return new MQOutboxDispatchResult(claimed.size(), published, rescheduled, dead, confirmationLost);
    }

    private void notifyObserver(Consumer<MQDeliveryObserver> action) {
        try {
            action.accept(observer);
        } catch (RuntimeException exception) {
            log.warn("MQ delivery observer failed and was ignored", exception);
        }
    }
}
