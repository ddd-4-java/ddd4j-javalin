package io.ddd4j.javalin.sample.orderoutbox;

import lombok.Data;

/**
 * Order Outbox 示例的运行配置 POJO。
 *
 * <p>对齐 ddd4j-boot 的 {@code OrderSampleProperties}：默认使用内存适配器
 * （{@code infrastructure=in-memory}），生产演示可切换为 {@code infrastructure=postgres}
 * 启用 PostgreSQL 事务写侧 + 事务 Outbox 调度。
 */
@Data
public class OrderOutboxSampleProperties {

    /** 基础设施选择：in-memory（默认）或 postgres。 */
    private String infrastructure = "in-memory";

    /** Kafka bootstrap 地址（示例保留字段；javalin sample 默认使用内存发布器）。 */
    private String kafkaBootstrapServers = "localhost:9092";

    /** Kafka 主题（示例保留字段）。 */
    private String kafkaTopic = "ddd4j.sample.order.events";

    /** Redis 主机（示例保留字段）。 */
    private String redisHost = "localhost";

    /** Redis 端口（示例保留字段）。 */
    private int redisPort = 6379;

    /** 每轮 Outbox 调度最多发布的消息数。 */
    private int outboxBatchSize = 100;

    /** Outbox 调度周期（毫秒）。 */
    private long outboxDelayMillis = 5000L;
}
