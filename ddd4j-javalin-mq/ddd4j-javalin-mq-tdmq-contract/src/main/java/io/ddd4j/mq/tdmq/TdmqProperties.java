package io.ddd4j.mq.tdmq;

import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 腾讯云 TDMQ 适配器配置。
 *
 * <p>{@link TdmqProperties} extends {@link MQProperties} —— 复用通用字段（namespace / defaultTopic /
 * autoAck / persist / retries 等），仅声明 TDMQ 专属字段。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TdmqProperties extends MQProperties {

    /**
     * TDMQ 服务地址（如 {@code http://pulsar-xxx.tdmq.ap-shanghai.qcloud.cn:8080}）。
     */
    private String serviceUrl;
    /**
     * TDMQ 租户（Pulsar 三级命名空间 {@code tenant/namespace/topic} 的第 1 段）。
     */
    private String tenant;
    /**
     * TDMQ AccessKey。
     */
    private String accessKey;
    /**
     * TDMQ SecretKey。
     */
    private String secretKey;
    /**
     * 默认消费组（listener 未声明 group 时回落）。
     */
    private String defaultGroup = "ddd4j-tdmq";
    /**
     * 是否自动启动消费者（false 时 initConsumer 仅注册不启动）。
     */
    private boolean autoStartConsumers = true;
    /**
     * 消费异常时是否重投。
     */
    private boolean requeueOnError = true;
}
