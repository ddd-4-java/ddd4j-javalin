package io.ddd4j.mq.ons;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Objects;
import java.util.Properties;

/**
 * 阿里云 ONS 适配器配置（纯 Java，零 Spring 依赖）。
 *
 * <p>{@link OnsProperties} extends {@link MQProperties} —— 复用通用字段（namespace / defaultTopic /
 * autoAck / persist / retries / producerGroup 等），仅声明 ONS 专属字段。
 *
 * <p>需要阿里云 AccessKey、Topic、ProducerId、ConsumerId 等。ONS 是阿里云 RocketMQ，提供原生
 * subscription 表达式 tag 过滤（见 {@link OnsMQClient}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnsProperties extends MQProperties {

    /**
     * ONS NameServer 地址（阿里云控制台获取）。
     */
    private String nameSrvAddr = "http://onsaddr-internet.aliyun.com:80/rocketmq/nsaddr4client-internet";
    /**
     * 阿里云 RAM AccessKey。
     */
    private String accessKey;
    /**
     * 阿里云 RAM SecretKey。
     */
    private String secretKey;
    /**
     * Producer ID（ONS 控制台创建）。
     */
    private String producerId;
    /**
     * Consumer ID（ONS 控制台创建）。
     */
    private String consumerId;
    /**
     * ONS Topic（无显式 listener.topic 时回落）。
     */
    private String topic;
    /**
     * 默认 tag 表达式（监听侧若 MQListener.tags 为空则用此值）。
     */
    private String defaultTag = "*";
    /**
     * Long polling 大小（KB）。
     */
    private int consumeMessageBatchMaxSize = 32;
    /**
     * 消费线程数。
     */
    private int consumeThreadCount = 20;
    /**
     * 最大重试次数。
     */
    private int maxReconsumeTimes = 3;

    /**
     * 构建 ONS 会话 {@link Properties}（AccessKey / NameServer / Group / Instance）。
     *
     * <p>namespace 取自父类 {@link MQProperties#getNamespace()}（对应 ONS InstanceId）。
     *
     * @param groupName 分组名（{@code null} 时用 {@code DEFAULT_GROUP}）
     * @return ONS 客户端配置 Properties
     */
    public Properties sessionProperties(String groupName) {
        Properties p = new Properties();
        p.setProperty(PropertyKeyConst.AccessKey, accessKey);
        p.setProperty(PropertyKeyConst.SecretKey, secretKey);
        p.setProperty(PropertyKeyConst.NAMESRV_ADDR, nameSrvAddr);
        p.setProperty(PropertyKeyConst.GROUP_ID, Objects.isNull(groupName) ? "DEFAULT_GROUP" : groupName);
        String ns = getNamespace();
        if (Objects.nonNull(ns) && !StrKit.isBlank(ns)) {
            p.setProperty(PropertyKeyConst.INSTANCE_ID, ns);
        }
        return p;
    }

    /**
     * 监听侧订阅 tag 表达式：tag 为空时回落到 {@link #defaultTag}。
     *
     * @param tag 监听器首个正向 tag（可为 null）
     * @return ONS subscription 表达式（如 {@code "*"} 或具体 tag）
     */
    public String subscriptionExpression(String tag) {
        return (Objects.isNull(tag) || StrKit.isBlank(tag)) ? defaultTag : tag;
    }
}
