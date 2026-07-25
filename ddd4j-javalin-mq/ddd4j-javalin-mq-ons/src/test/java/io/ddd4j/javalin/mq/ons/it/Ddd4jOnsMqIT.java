package io.ddd4j.javalin.mq.ons.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@JunitJupiterTestContainers
@Disabled("ONS is an Alibaba Cloud managed service; no open-source Testcontainers image available")
class Ddd4jOnsMqIT {

    @Test
    void shouldBeDisabled() {
        // Placeholder until a LocalStack-equivalent or Alibaba-provided fixture becomes
        // available; the production wiring is verified by the sample module instead.
    }
}