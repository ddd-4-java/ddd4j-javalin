package io.ddd4j.mq.event;

/**
 * MQ 事件持久化端口。
 *
 * <p>ddd4j 不提供默认实现，业务侧可按需注册实现以落库、写审计表或投递到外部存储。
 *
 * @param <T> 事件类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface MQEventStorer<T extends MQEvent> {

    /**
     * 持久化 MQ 事件。
     *
     * @param event 已反序列化事件
     */
    void store(T event);
}
