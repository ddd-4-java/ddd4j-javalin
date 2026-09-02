package io.ddd4j.mq;

import io.ddd4j.kit.lang.StrKit;

import lombok.Data;

import java.util.Objects;

/**
 * ddd4j MQ 主配置（前缀 {@code ddd4j.mq}）。
 *
 * <p>融合 base-mq {@code BaseMQProperties} 的连接字段（server/username/password/exchange 等）
 * 与 ddd4j 原有的配置，供 {@link MQClient} 各实现使用。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
public class MQProperties {

    /**
     * 是否启用 MQ
     */
    private boolean enabled = false;

    /**
     * 分区路由策略（MQClient.PartitionKeyStrategy 枚举）。详见父类 Javadoc。
     */
    private MQClient.PartitionKeyStrategy partitionKeyStrategy = MQClient.PartitionKeyStrategy.TAG_TENANT;

    /**
     * 当前 Broker 实现（配置字符串，如 kafka/rocket/rabbit/redis/redisStream），
     * 一个应用只用一套 MQ 发布和订阅。
     */
    private String broker = "none";

    /**
     * 服务地址（如 {@code host:port}）
     */
    private String server = "";

    /**
     * 命名空间 / 环境前缀，用于环境隔离等场景（如 UAT/生产/租户共用一个 MQ）
     */
    private String namespace = "";

    /**
     * 是否持久化到本地（需注册 {@link io.ddd4j.mq.event.MQEventStorer} 实现）
     */
    private boolean persist = false;

    /**
     * 序列化器标识（对应 {@link io.ddd4j.mq.event.MQEventSerialization} 实现的 Bean 名）
     */
    private String serialization = "json";

    /**
     * 是否自动确认（autoAck=true 时由 broker 自动 ack，false 时手动 ack）
     */
    private boolean autoAck = false;

    /**
     * 发送失败重试次数
     */
    private int retries = 0;

    /**
     * 用户名（鉴权）
     */
    private String username = "";

    /**
     * 密码（鉴权）
     */
    private String password = "";

    /**
     * 数据库（Redis 场景）
     */
    private String database;

    /**
     * 生产者组（RocketMQ 等需要）
     */
    private String producerGroup = "DEFAULT";

    /**
     * {@code MQEvent.publish()} 默认 topic
     */
    private String defaultTopic = "DEFAULT";

    /**
     * 交换机（RabbitMQ 场景）
     */
    private String exchange = "";

    /**
     * 带命名空间前缀的拼接便捷方法。
     *
     * @param sep 分隔符
     * @return {@code namespace + sep}（namespace 为空时返回空串）
     */
    public String namespace(String sep) {
        if (StrKit.isNotEmpty(namespace)) {
            return namespace + sep;
        }
        return "";
    }
}
