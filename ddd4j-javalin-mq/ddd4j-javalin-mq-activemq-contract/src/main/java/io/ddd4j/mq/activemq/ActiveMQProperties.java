package io.ddd4j.mq.activemq;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ActiveMQ Artemis 适配器配置（纯 Java，零 Spring 依赖）。
 *
 * <p>对应 {@code org.apache.activemq:artemis-client} 2.x 原生 JMS 客户端。
 * 同时兼容 {@link BrokerType#ACTIVEMQ}（历史 Classic v5 通过单独引入 {@code activemq-client} 5.x 也可适配）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActiveMQProperties extends MQProperties {

    /**
     * Broker URL（例：{@code tcp://host:61616} 或 {@code failover:(tcp://...)}）。
     */
    private String brokerUrl = "tcp://localhost:61616";
    /**
     * 认证用户名
     */
    private String username;
    /**
     * 认证密码
     */
    private String password;
    /**
     * 客户端 ID 前缀
     */
    private String clientIdPrefix = "ddd4j-mq-";
    /**
     * 是否在注册时自动创建 queues / topics（Artemis 默认按需自动创建）。
     */
    private boolean autoDeclare = true;
    /**
     * 消息默认持久化。
     */
    private boolean durable = true;

}
