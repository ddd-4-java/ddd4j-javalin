package io.ddd4j.javalin.mq.core.fixture.valid;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;

/** Scanner fixture with deterministic method names. */
public class ValidMqListener {

    @MQEventListener(topic = "orders.updated", group = "production")
    public void onUpdated(MQEvent event) {
    }

    @MQEventListener(topic = "orders.created", group = "production")
    public void onCreated(MQEvent event) {
    }
}
