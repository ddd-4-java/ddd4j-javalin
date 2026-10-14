package io.ddd4j.javalin.mq.core.fixture.valid;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;

/**
 * Scanner fixture with deterministic method names.
 */
public class ValidMqListener {

    /**
     * 接收到更新事件时的回调。
     * @param event 事件消息。
     */
    @MQEventListener(topic = "orders.updated", group = "production")
    public void onUpdated(MQEvent event) {
    }

    /**
     * 接收到创建事件时的回调。
     * @param event 事件消息。
     */
    @MQEventListener(topic = "orders.created", group = "production")
    public void onCreated(MQEvent event) {
    }
}
