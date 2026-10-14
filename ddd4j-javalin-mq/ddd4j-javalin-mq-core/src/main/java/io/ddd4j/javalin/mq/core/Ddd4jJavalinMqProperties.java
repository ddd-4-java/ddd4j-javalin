package io.ddd4j.javalin.mq.core;

import lombok.Getter;
import lombok.Setter;

/**
 * Javalin MQ 生命周期策略。
 */
@Getter
@Setter
public class Ddd4jJavalinMqProperties {

    private Role role = Role.BOTH;
    private boolean requireListeners = true;
    /**
     * 应用使用 MQ 的角色。
     */
    public enum Role {
        /** 仅作为生产者，不初始化消费者。 */
        PRODUCER_ONLY,
        /** 仅作为消费者，不初始化生产者。 */
        CONSUMER_ONLY,
        /** 既作为生产者也作为消费者。 */
        BOTH;

        boolean produces() {
            return this == PRODUCER_ONLY || this == BOTH;
        }

        boolean consumes() {
            return this == CONSUMER_ONLY || this == BOTH;
        }
    }
}
