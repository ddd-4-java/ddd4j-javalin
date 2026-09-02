package io.ddd4j.mq.sqs;

import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;
import java.util.Objects;

/**
 * AWS SQS 适配器配置（纯 Java，零 Spring 依赖）。
 *
 * <p>{@link SqsProperties} extends {@link MQProperties} —— 复用通用字段（namespace / defaultTopic /
 * autoAck / persist / retries 等），仅声明 SQS 专属字段。
 *
 * <p>SQS 没有 topic/tag 概念：{@code MQListener.topic} 直接被解释为 queueUrl。
 * 多 queueUrl 场景下，业务可在发布/消费端各自注入 {@code Map<String,String>} 进行路由。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SqsProperties extends MQProperties {

    /**
     * AWS 区域。
     */
    private String region = "us-east-1";
    /**
     * 可选：自定义端点（LocalStack、LocalStack community）。
     */
    private String endpointOverride;
    /**
     * AWS AccessKey（留空则走 {@link DefaultCredentialsProvider}）。
     */
    private String accessKey;
    /**
     * AWS SecretKey。
     */
    private String secretKey;
    /**
     * 单次 long poll 等待时长（秒）。
     */
    private int waitTimeSeconds = 20;
    /**
     * 接收批大小（最大 10）。
     */
    private int maxNumberOfMessages = 10;
    /**
     * Visibility timeout（消息从队列隐藏的最大时间）。
     */
    private int visibilityTimeoutSeconds = 30;
    /**
     * 错误 nack 时是否重置 visibility 让消息立即被另一消费者接收。
     */
    private boolean requeueOnNack = true;
    /**
     * Long poll 期间轮询间隔（毫秒）。
     */
    private long pollIntervalMs = 200L;

    /**
     * 构建 AWS 凭证 Provider（有 static 配置则用 StaticCredentialsProvider，否则 DefaultCredentialsProvider）。
     *
     * @return 凭证 Provider
     */
    public AwsCredentialsProvider credentialsProvider() {
        if (Objects.nonNull(accessKey) && !StrKit.isBlank(accessKey)
                && Objects.nonNull(secretKey) && !StrKit.isBlank(secretKey)) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.create();
    }

    /**
     * 构建原生 {@link SqsClient}。
     *
     * @return 已配置好的 SqsClient
     */
    public SqsClient client() {
        SqsClientBuilder b = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (Objects.nonNull(endpointOverride) && !StrKit.isBlank(endpointOverride)) {
            b.endpointOverride(URI.create(endpointOverride));
        }
        return b.build();
    }
}
