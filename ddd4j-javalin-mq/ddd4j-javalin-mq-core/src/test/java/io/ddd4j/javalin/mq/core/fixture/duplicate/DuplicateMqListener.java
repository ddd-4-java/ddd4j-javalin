package io.ddd4j.javalin.mq.core.fixture.duplicate;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;

/**
 * Scanner fixture with an intentionally duplicated route.
 */
public class DuplicateMqListener {

    /**
     * 接收到第一条重复事件时的回调。
     * @param event 事件消息。
     */
    @MQEventListener(topic = "orders.duplicate", group = "production")
    public void first(MQEvent event) {
    }

    /**
     * 接收到第二条重复事件时的回调。
     * @param event 事件消息。
     */
    @MQEventListener(topic = "orders.duplicate", group = "production")
    public void second(MQEvent event) {
    }
}
