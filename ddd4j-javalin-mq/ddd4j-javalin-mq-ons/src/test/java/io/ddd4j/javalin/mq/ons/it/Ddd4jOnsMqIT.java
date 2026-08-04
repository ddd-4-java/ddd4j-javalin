package io.ddd4j.javalin.mq.ons.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration test for ONS (Alibaba Cloud managed RocketMQ-compatible protocol).
 *
 * <p>ONS has no open-source Testcontainers image — the broker is only reachable with
 * Alibaba Cloud credentials, so the class stays disabled.
 *
 * <p>Upgrade path (once a fixture becomes available, e.g. a LocalStack-style emulator or
 * an Aliyun-provided container):
 * <ol>
 *   <li>Boot a container exposing the namesrv port 9876 and broker ports 10909/10911
 *       (ONS speaks the RocketMQ remoting protocol; {@code OnsMQClient} wraps the
 *       {@code com.aliyun.openservices:ons-client} SDK which needs {@code ONSAddr} /
 *       access-key config).</li>
 *   <li>Point {@code io.ddd4j.mq.ons.OnsProperties} at it and drive the round trip like
 *       {@code Ddd4jRocketMqIT}: construct {@code OnsMQClient(OnsProperties)}, wire it via
 *       {@code Ddd4jOnsMqGuiceModule}, call {@code MQClient#init} with an
 *       {@code @MQEventListener} bean, then {@code MQEvent.publish()} → await consumption
 *       with Awaitility.</li>
 * </ol>
 */
@Tag("integration")
@JunitJupiterTestContainers
@Disabled("ONS is an Alibaba Cloud managed service; no open-source Testcontainers image available")
class Ddd4jOnsMqIT {

    @Test
    void shouldBeDisabled() {
        // Placeholder until a LocalStack-equivalent or Alibaba-provided fixture becomes
        // available. Upgrade path: mirror Ddd4jRocketMqIT (ONS uses the RocketMQ remoting
        // protocol) — boot a namesrv+broker container, construct OnsMQClient(OnsProperties),
        // then MQClient#init → MQEvent.publish() → await consumption on an @MQEventListener bean.
    }
}
