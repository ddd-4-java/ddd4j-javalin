package io.ddd4j.javalin.mq.tdmq.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration test for TDMQ (Tencent Cloud managed messaging service).
 *
 * <p>TDMQ has no open-source Testcontainers image — the broker is only reachable with
 * Tencent Cloud credentials, so the class stays disabled.
 *
 * <p>Upgrade path: {@code TdmqMQClient} delegates publish/subscribe to injected
 * {@code BrokerPublisher} / {@code BrokerSubscriber} SPI implementations (business-side
 * Tencent SDK wrappers), so a local round trip is possible by injecting an in-process
 * broker (the client already ships an in-memory bus for local development). When a
 * self-hostable emulator appears (TDMQ RocketMQ/Pulsar variants), boot it, wrap its SDK
 * in the two SPI interfaces, then wire {@code Ddd4jTdmqMqGuiceModule} and run
 * {@code MQClient#init} → {@code MQEvent.publish()} → await consumption on an
 * {@code @MQEventListener} bean, exactly like the other round-trip ITs.
 */
@Tag("integration")
@JunitJupiterTestContainers
@Disabled("TDMQ is a Tencent Cloud managed service; no open-source Testcontainers image available")
class Ddd4jTdmqMqIT {

    @Test
    void shouldBeDisabled() {
        // Placeholder until a self-hostable emulator or official fixture becomes available.
        // Upgrade path: inject BrokerPublisher/BrokerSubscriber backed by the Tencent SDK
        // (or the built-in in-memory bus), wire Ddd4jTdmqMqGuiceModule and assert a
        // MQEvent publish → consume round trip like the other broker ITs.
    }
}
