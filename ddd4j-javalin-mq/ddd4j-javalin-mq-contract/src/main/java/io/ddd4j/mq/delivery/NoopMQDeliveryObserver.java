package io.ddd4j.mq.delivery;

/**
 * 默认的无操作可靠消息观察器。
 *
 * <p>用于未接入可观测性扩展的场景，保证可靠消息流程不需要额外运行时依赖。
 */
public enum NoopMQDeliveryObserver implements MQDeliveryObserver {

    /** 单例实例。 */
    INSTANCE
}
