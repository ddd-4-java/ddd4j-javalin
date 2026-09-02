package io.ddd4j.mq.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.ddd4j.core.constant.ContextConstants;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * MQ 事件基类（纯 Java，零 Spring 依赖）。
 *
 * <p>业务事件继承本类后，通过 {@link #publish()} 发布到 MQ Broker。
 * 仅走 MQ 通道，进程内事件请使用 {@code io.ddd4j.core.ddd.event.DomainEvent}。
 *
 * <p>发布机制（多 broker 路由）：{@link #publish(String, String, String)} 从 {@link BaseContext}
 * 查找 key 为 {@link #MQ_EVENT_PUBLISHER} 的 {@code Map<String, Consumer<MQEvent>>}，
 * 按 {@link #broker} 字段（或全局 {@link MQProperties#getBroker()} 配置）匹配对应的 broker 生产者。
 * 每个 {@link MQClient} 在 {@code initProducer} 后以 {@link MQClient#impl()} 为 key 注册。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public class OrderCreatedEvent extends MQEvent {
 *     private String orderId;
 *     private BigDecimal amount;
 * }
 *
 * // 1. 走全局默认 broker（配置 ddd4j.mq.broker=kafka）
 * new OrderCreatedEvent("OBS-001", BigDecimal.valueOf(99.9)).publish();
 *
 * // 2. 指定推送到 redisStream
 * new OrderCreatedEvent("OBS-001", BigDecimal.valueOf(99.9)).broker("redisStream").publish();
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.0.0
 */
@Slf4j
@Data
@SuppressWarnings("unchecked")
public class MQEvent implements Serializable {

    /**
     * {@link BaseContext} key：MQ 事件发布者 Map（{@code Map<String, Consumer<MQEvent>>}）。
     * key = {@link MQClient#impl()} 返回值（如 {@code "kafka"} / {@code "rocket"} / {@code "redisStream"}），
     * value = {@link MQClient#initProducer(MQProperties)} 返回的发布函数。
     */
    public static final String MQ_EVENT_PUBLISHER = "MQEventPublisher";
    /**
     * {@link BaseContext} key：MQ 配置（{@link MQProperties}），
     * 用于获取 defaultTopic / namespace / broker 默认值。
     */
    public static final String MQ_PROPERTIES = "MQProperties";
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 消息 ID，默认当前时间戳
     */
    protected String msgId;
    /**
     * 主题，配置 {@code ddd4j.mq.default-topic} 后无须每次指定
     */
    protected String topic;
    /**
     * 标签，只支持单个标签，多标签需要分开发送
     */
    protected String tag;
    /**
     * namespace/topic/tag 拼接符，为空时由各 broker 实现决定默认值
     */
    protected String concat;
    /**
     * 租户 ID，默认从线程上下文获取（外部 JSON 常用 tenant_id）
     */
    @JsonAlias("tenant_id")
    protected String tenantId;
    /**
     * 目标 broker 标识（如 {@code "kafka"} / {@code "rocket"} / {@code "redisStream"}）。
     * <p>为空时走全局默认 broker（{@link MQProperties#getBroker()} 配置）。
     */
    protected String broker;
    /**
     * 命名空间，配置 {@code ddd4j.mq.namespace} 后无须每次指定
     */
    private String namespace;

    /**
     * 策略匹配：supports 参数来源于 {@code @MQEventListener.supports}。
     */
    public boolean supports(List<String> supports) {
        return supports.contains(match());
    }

    /**
     * 策略匹配项，默认 {@code "*"}（匹配所有监听器），子类可覆写。
     */
    public String match() {
        return "*";
    }

    // ========================= 发布 =========================

    public void publish() {
        publish(getTopic(), getTag(), getTenantId());
    }

    public void publish(String topic) {
        publish(topic, getTag(), getTenantId());
    }

    public void publish(String topic, String tag) {
        publish(topic, tag, getTenantId());
    }

    /**
     * 发布 MQ 事件。
     *
     * <p>从 {@link BaseContext} 查找 {@code Map<String, Consumer<MQEvent>>}（由各 {@link MQClient} 注册），
     * 按以下优先级匹配目标 broker 生产者：
     * <ol>
     *   <li>{@link #broker} 字段非空 → 用此值作 key 查找</li>
     *   <li>{@link MQProperties#getBroker()} 全局配置非空且非 {@code "none"} → 用此值作 key 查找</li>
     *   <li>仅注册了一个 broker → 直接用（便捷场景）</li>
     *   <li>都找不到 → warn 日志，事件不发布</li>
     * </ol>
     */
    public void publish(String topic, String tag, String tenantId) {
        setTopic(topic);
        if (Objects.isNull(getTopic())) {
            MQProperties props = BaseContext.get(MQ_PROPERTIES);
            setTopic(Objects.nonNull(props) ? props.getDefaultTopic() : "DEFAULT");
        }
        setTag(tag);
        setTenantId(Objects.nonNull(tenantId) ? tenantId : ThreadContext.get(ContextConstants.TENANT_ID));
        setMsgId(Objects.isNull(getMsgId()) ? UUID.randomUUID().toString() : getMsgId());

        Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQ_EVENT_PUBLISHER);
        if (Objects.isNull(publishers) || publishers.isEmpty()) {
            log.warn("No MQEventPublisher registered, event [{}] not published", getTopic());
            return;
        }

        // 按优先级查找目标 broker
        String targetBroker = getBroker();
        if (StrKit.isEmpty(targetBroker)) {
            MQProperties props = BaseContext.get(MQ_PROPERTIES);
            targetBroker = Objects.nonNull(props) ? props.getBroker() : null;
        }
        Consumer<MQEvent> publisher = null;
        if (StrKit.isNotEmpty(targetBroker) && !"none".equalsIgnoreCase(targetBroker)) {
            publisher = publishers.get(targetBroker);
        }
        if (Objects.isNull(publisher) && publishers.size() == 1) {
            publisher = publishers.values().iterator().next();
        }
        if (Objects.nonNull(publisher)) {
            publisher.accept(this);
        } else {
            log.warn("No MQEventPublisher found for broker=[{}], event [{}] not published. Registered: {}",
                    targetBroker, getTopic(), publishers.keySet());
        }
    }

    /**
     * 链式设置租户 ID。
     */
    public <T extends MQEvent> T tenantId(String tenantId) {
        this.tenantId = tenantId;
        return (T) this;
    }

    /**
     * 链式设置目标 broker。
     *
     * @param broker broker 标识（如 {@code "kafka"} / {@code "rocket"} / {@code "redisStream"}）
     * @return this
     */
    public <T extends MQEvent> T broker(String broker) {
        this.broker = broker;
        return (T) this;
    }
}
