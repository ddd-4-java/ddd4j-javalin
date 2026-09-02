package io.ddd4j.mq.kafka;

import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Properties;

/**
 * Kafka adapter configuration.
 *
 * <p>{@link KafkaMQProperties} extends {@link MQProperties} —— 复用通用字段（namespace / concat /
 * defaultTopic / autoAck / persist / retries / username / password 等），仅声明 Kafka 专属字段。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KafkaMQProperties extends MQProperties {

    /**
     * 默认分区数（自动创建 topic）。
     */
    public static final int DEFAULT_TOPIC_PARTITIONS = 3;
    /**
     * 默认副本数。
     */
    public static final short DEFAULT_TOPIC_REPLICATION = 1;

    /**
     * Kafka broker bootstrap servers（独立于父类 server 以遵循 Kafka 命名约定）。
     */
    private String bootstrapServers = "localhost:9092";
    private String clientId = "ddd4j-mq-kafka";
    private String groupIdPrefix = "ddd4j";
    private Duration pollTimeout = Duration.ofSeconds(1);
    private boolean autoStartConsumers = true;

    /**
     * 是否在 initProducer 时通过 AdminClient 自动创建缺失的 topic（借鉴 4）。
     */
    private boolean autoCreateTopics = true;

    /**
     * 自动创建 topic 时的分区数（仅当 autoCreateTopics=true 生效）。
     */
    private int defaultTopicPartitions = DEFAULT_TOPIC_PARTITIONS;

    /**
     * 自动创建 topic 时的副本数（仅当 autoCreateTopics=true 生效，生产集群建议 ≥3）。
     */
    private short defaultTopicReplication = DEFAULT_TOPIC_REPLICATION;

    /**
     * Producer 配置（对齐 ProducerConfig 常量名）
     */
    public Properties producerProperties() {
        Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432L);
        return properties;
    }

    /**
     * AdminClient 配置
     */
    public Properties adminProperties() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(AdminClientConfig.CLIENT_ID_CONFIG, clientId + "-admin");
        return properties;
    }

    /**
     * Consumer 配置
     */
    public Properties consumerProperties(String groupId) {
        Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId + "-consumer");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
        properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        return properties;
    }
}
