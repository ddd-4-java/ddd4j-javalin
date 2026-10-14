package io.ddd4j.javalin.mq.core;

import lombok.Getter;
import lombok.Setter;

/**
 * Javalin MQ 生命周期策略。
 */
@Getter
@Setter
public class Ddd4jJavalinMqProperties {

    /**
     * 应用使用 MQ 的角色。
     */
    public enum Role {
        /** 仅作为生产者发布事件。 */
        PRODUCER_ONLY,
        /** 仅作为消费者订阅事件。 */
        CONSUMER_ONLY,
        /** 同时作为生产者与消费者。 */
        BOTH;

        /**
         * 判断当前角色是否需要发布事件。
         *
         * @return 为 {@code true} 表示需要生产事件。
         */
        boolean produces() {
            return this == PRODUCER_ONLY || this == BOTH;
        }

        /**
         * 判断当前角色是否需要订阅事件。
         *
         * @return 为 {@code true} 表示需要消费事件。
         */
        boolean consumes() {
            return this == CONSUMER_ONLY || this == BOTH;
        }
    }

    private Role role = Role.BOTH;
    private boolean requireListeners = true;
}
