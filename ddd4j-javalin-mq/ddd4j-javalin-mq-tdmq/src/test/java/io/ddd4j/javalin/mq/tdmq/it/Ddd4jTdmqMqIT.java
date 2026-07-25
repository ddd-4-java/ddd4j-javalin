package io.ddd4j.javalin.mq.tdmq.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@JunitJupiterTestContainers
@Disabled("TDMQ is a Tencent Cloud managed service; no open-source Testcontainers image available")
class Ddd4jTdmqMqIT {

    @Test
    void shouldBeDisabled() {
        // Placeholder; production wiring verified by sample module.
    }
}