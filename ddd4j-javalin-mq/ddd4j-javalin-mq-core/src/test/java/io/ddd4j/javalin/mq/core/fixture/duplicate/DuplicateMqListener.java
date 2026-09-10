package io.ddd4j.javalin.mq.core.fixture.duplicate;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;

/** Scanner fixture with an intentionally duplicated route. */
public class DuplicateMqListener {

    @MQEventListener(topic = "orders.duplicate", group = "production")
    public void first(MQEvent event) {
    }

    @MQEventListener(topic = "orders.duplicate", group = "production")
    public void second(MQEvent event) {
    }
}
