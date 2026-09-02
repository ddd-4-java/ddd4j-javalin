package io.ddd4j.mq.pulsar;

import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Apache Pulsar 适配器配置（纯 Java，零 Spring 依赖）。
 *
 * <p>{@link PulsarProperties} extends {@link MQProperties} —— 复用通用字段（namespace /
 * defaultTopic / autoAck / persist / retries / partitionKeyStrategy 等），仅声明 Pulsar 专属字段。
 *
 * <p>版本通过 {@code pulsar-bom} 与 {@code ${pulsar.version}} 对齐。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PulsarProperties extends MQProperties {

    /**
     * Pulsar broker service URL（如 {@code pulsar://host:6650}）。
     */
    private String serviceUrl = "pulsar://localhost:6650";
    /**
     * Pulsar 租户（Pulsar {@code tenant/namespace/topic} 三级命名空间的第 1 段）。
     */
    private String tenant = "public";
    /**
     * Authentication token（可选）。
     */
    private String authToken;
    /**
     * 客户端操作超时（ms）。
     */
    private long operationTimeoutMs = 30_000L;
    /**
     * IO 线程数。
     */
    private int ioThreads = 1;
    /**
     * Listener 线程数。
     */
    private int listenerThreads = 1;
    /**
     * Subscription type: Exclusive / Shared / Failover / Key_Shared.
     */
    private String subscriptionType = "Shared";
    /**
     * Subscription name (per consumer).
     */
    private String subscriptionName = "ddd4j-mq-subscription";
    /**
     * Negative ack redelivery delay (ms).
     */
    private long negativeAckRedeliveryDelayMs = 1_000L;

    /**
     * 构建原生 {@link PulsarClient}。
     *
     * @return 已配置好的 PulsarClient
     * @throws Exception 构建异常
     */
    public PulsarClient client() throws Exception {
        ClientBuilder b = PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .operationTimeout(Math.toIntExact(operationTimeoutMs), TimeUnit.MILLISECONDS)
                .ioThreads(ioThreads)
                .listenerThreads(listenerThreads);
        if (Objects.nonNull(authToken) && !StrKit.isBlank(authToken)) {
            b.authentication(AuthenticationFactory.token(authToken));
        }
        return b.build();
    }

    /**
     * 拼接 Pulsar 物理 topic：{@code tenant/namespace/topic[:tag]}。
     *
     * <p>namespace 段取自父类 {@link MQProperties#getNamespace()}。
     *
     * @param topic 逻辑 topic（不可为 null）
     * @param tag   tag（可为 null，null 时不追加 {@code :tag}）
     * @return 物理 topic 全名
     */
    public String physicalTopic(String topic, String tag) {
        Objects.requireNonNull(topic, "topic");
        return tenant + "/" + getNamespace() + "/"
                + (Objects.isNull(tag) || StrKit.isBlank(tag) ? topic : topic + ":" + tag);
    }
}
