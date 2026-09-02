package io.ddd4j.mq.delivery;

import io.ddd4j.kit.lang.StrKit;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 将 Inbox 去重和消费者业务处理串联的协调器。
 *
 * <p>调用此类的方法与业务处理器必须由同一个事务拦截器包裹：首次记录和业务写入一起提交，
 * 处理器抛异常时一起回滚，重复消息则不再执行业务处理但仍应由调用方 ACK。
 */
@Slf4j
public final class MQInboxProcessor {

    private final MQInboxStore store;
    private final String consumerId;
    private final MQDeliveryObserver observer;

    public MQInboxProcessor(MQInboxStore store, String consumerId) {
        this(store, consumerId, NoopMQDeliveryObserver.INSTANCE);
    }

    /**
     * 创建带投递结果观察器的 Inbox 处理器。
     *
     * @param store Inbox 持久化端口
     * @param consumerId 稳定消费者标识
     * @param observer 旁路观测实现
     */
    public MQInboxProcessor(MQInboxStore store, String consumerId, MQDeliveryObserver observer) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        if (StrKit.isBlank(consumerId)) {
            throw new IllegalArgumentException("consumerId must not be blank");
        }
        this.consumerId = consumerId;
        this.observer = Objects.requireNonNull(observer, "observer must not be null");
    }

    /**
     * 处理一条可靠消息。
     *
     * @param messageId 生产端稳定消息标识
     * @param processedAt 当前处理时间
     * @param handler 业务处理器
     * @return {@code true} 表示首次处理，{@code false} 表示重复消息
     */
    public boolean process(String messageId, Instant processedAt, Runnable handler) {
        if (StrKit.isBlank(messageId)) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        Objects.requireNonNull(processedAt, "processedAt must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        try {
            if (!store.recordIfAbsent(consumerId, messageId, processedAt)) {
                notifyObserver(deliveryObserver -> deliveryObserver.onInboxDuplicate(consumerId, messageId));
                return false;
            }
            handler.run();
            notifyObserver(deliveryObserver -> deliveryObserver.onInboxProcessed(consumerId, messageId));
            return true;
        } catch (RuntimeException exception) {
            notifyObserver(deliveryObserver -> deliveryObserver.onInboxFailed(consumerId, messageId));
            throw exception;
        }
    }

    private void notifyObserver(Consumer<MQDeliveryObserver> action) {
        try {
            action.accept(observer);
        } catch (RuntimeException exception) {
            log.warn("MQ delivery observer failed and was ignored", exception);
        }
    }
}
