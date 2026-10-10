package io.ddd4j.javalin.mq.core;

import lombok.Getter;
import lombok.Setter;

/** Javalin MQ 生命周期策略。 */
@Getter
@Setter
public class Ddd4jJavalinMqProperties {

    /** 应用使用 MQ 的角色。 */
    public enum Role {
        /** 仅生产：启动时不扫描监听器，生命周期只维护生产端资源。 */
        PRODUCER_ONLY,
        /** 仅消费：不初始化生产端，要求至少注册一个监听器。 */
        CONSUMER_ONLY,
        /** 双角色（默认）：生产与消费资源均维护。 */
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
