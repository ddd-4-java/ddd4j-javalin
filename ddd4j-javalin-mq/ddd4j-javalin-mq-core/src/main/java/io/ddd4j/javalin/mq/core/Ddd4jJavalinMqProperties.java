package io.ddd4j.javalin.mq.core;

import lombok.Getter;
import lombok.Setter;

/** Javalin MQ 生命周期策略。 */
@Getter
@Setter
public class Ddd4jJavalinMqProperties {

    /** 应用使用 MQ 的角色。 */
    public enum Role {
        PRODUCER_ONLY,
        CONSUMER_ONLY,
        BOTH;

        boolean produces() {
            return this == PRODUCER_ONLY || this == BOTH;
        }

        boolean consumes() {
            return this == CONSUMER_ONLY || this == BOTH;
        }
    }

    private Role role = Role.BOTH;
    private boolean requireListeners = true;
}
