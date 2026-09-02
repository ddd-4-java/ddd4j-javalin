package io.ddd4j.mq;

import io.ddd4j.core.constant.ContextConstants;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.event.MQEventStorer;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.Acknowledgment;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * MQ 客户端契约（纯 Java，零 Spring 依赖）。100% 参考实现 base-mq {@code MQClient}。
 *
 * <p>接入新的 MQ 只需实现本接口，每个 broker 模块最终 3 个文件：
 * {@code XxxClient}（impl MQClient）+ {@code XxxAcknowledgment} + {@code XxxProperties}。
 *
 * <h3>核心流程</h3>
 * <ol>
 *   <li><b>发布</b>：{@link #initProducer(MQProperties)} 返回 {@link Consumer<MQEvent>}，
 *       由 {@link #init(List, MQProperties, MQEventSerialization, MQEventStorer)} 注册到 {@link BaseContext}
 *       key 为 {@link MQEvent#MQ_EVENT_PUBLISHER}。{@link MQEvent#publish()} 通过该 Consumer 推送到底层生产者</li>
 *   <li><b>消费</b>：{@link #initConsumer(MQListener, MQProperties)} 建立原生消费者，
 *       收到消息后反序列化为 {@link MQEvent}，调 {@link #consume(MQListener, MQEvent, Acknowledgment)} 统一处理，
 *       最终反射调用 {@code @MQEventListener} 标注的监听方法</li>
 *   <li><b>确认</b>：各实现构建 broker 专属 {@link Acknowledgment} 传入 consume，
 *       实现不同级别的 ack（单条/批量/requeue/DLQ），解决 base-mq ack 能力弱的问题</li>
 * </ol>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public interface MQClient extends AutoCloseable {

    /**
     * {@link BaseContext} key：MQ 序列化器
     */
    String MQ_SERIALIZATION = MQEvent.MQ_EVENT_PUBLISHER + ".serialization";
    /**
     * {@link BaseContext} key：MQ 事件持久化器
     */
    String MQ_STORER = MQEvent.MQ_EVENT_PUBLISHER + ".storer";

    /**
     * 简单转义单引号（防止 tags 内含单引号破坏 selector 解析）。
     */
    private static String escape(String s) {
        return s.replace("'", "''");
    }

    /**
     * 反射调用异常解包（对齐 base-mq）。
     */
    private static Throwable unwrap(Exception ex) {
        Throwable cause = ex.getCause();
        if (Objects.nonNull(cause) && Objects.nonNull(cause.getCause())) {
            return cause.getCause();
        }
        return Objects.nonNull(cause) ? cause : ex;
    }

    /**
     * @return MQ 实现标识（如 {@code "kafka"} / {@code "rocket"} / {@code "rabbit"} / {@code "redis"} / {@code "redisStream"}）
     */
    String impl();

    /**
     * 整体初始化：注册依赖 → 初始化生产者 → 注册所有消费者。
     *
     * <p>由框架适配层（如 ddd4j-mq-spring）在应用就绪时调用。本方法会把 {@link #initProducer} 返回的
     * {@link Consumer<MQEvent>} 注册到 {@link BaseContext}，{@link MQEvent#publish()} 通过它推送消息。
     *
     * @param listeners     已扫描好的监听器列表
     * @param properties    MQ 配置（同时注册到 {@link BaseContext}，供 MQEvent 读 defaultTopic）
     * @param serialization 序列化器
     * @param storer        事件持久化器（可为 null）
     */
    default void init(List<MQListener> listeners, MQProperties properties,
                      MQEventSerialization serialization, MQEventStorer storer) {
        if (!properties.isEnabled() || !Objects.equals(properties.getBroker(), impl())) {
            return;
        }
        Logger log = logger();
        // 注册配置与依赖到 BaseContext
        BaseContext.inject(MQEvent.MQ_PROPERTIES, properties);
        BaseContext.inject(MQ_SERIALIZATION, serialization);
        if (Objects.nonNull(storer)) {
            BaseContext.inject(MQ_STORER, storer);
        }
        // 初始化生产者，注册到 publishers Map（key=impl()，允许多 broker 共存）
        Consumer<MQEvent> producer = initProducer(properties);
        if (Objects.nonNull(producer)) {
            log.info("Initializing MQEventPublisher for [{}]", impl());
            Map<String, Consumer<MQEvent>> publishers = BaseContext.get(MQEvent.MQ_EVENT_PUBLISHER);
            if (Objects.isNull(publishers)) {
                publishers = new ConcurrentHashMap<>();
                BaseContext.inject(MQEvent.MQ_EVENT_PUBLISHER, publishers);
            }
            publishers.put(impl(), producer);
        }
        // 初始化消费者
        log.info("Initializing MQEventListener for [{}]", impl());
        int success = 0;
        for (MQListener listener : listeners) {
            try {
                if (initConsumer(listener, properties)) {
                    success++;
                }
            } catch (Exception e) {
                log.error("Listen MQ [{}] failed!", listener.getRouteExpression(this.defaultConcat()), e);
            }
        }
        log.info("MQ [{}] listening {} listener(s)", impl(), success);

    }

    /**
     * 初始化生产者，返回 MQ 事件的发布函数。
     *
     * <p>返回的 {@link Consumer<MQEvent>} 会被注册到 {@link BaseContext}，
     * {@link MQEvent#publish()} 调用 {@code consumer.accept(event)} 把消息推送到 broker 生产者。
     *
     * @param properties MQ 配置
     * @return 发布函数，null 表示不提供发布能力
     */
    Consumer<MQEvent> initProducer(MQProperties properties);

    /**
     * 初始化单个消费者。
     *
     * <p>实现方在此建立 broker 原生消费者，收到消息后：
     * <ol>
     *   <li>用 {@link TagMatcher} 做 tag 过滤</li>
     *   <li>反序列化为 {@link MQEvent}（用 {@link #serialization()}）</li>
     *   <li>构建 broker 专属 {@link Acknowledgment}</li>
     *   <li>调用 {@link #consume(MQListener, MQEvent, Acknowledgment)} 完成统一消费</li>
     * </ol>
     *
     * @param listener   监听器定义
     * @param properties MQ 配置
     * @return 是否初始化成功
     * @throws Exception 初始化异常
     */
    boolean initConsumer(MQListener listener, MQProperties properties) throws Exception;

    /**
     * 启动（如统一启动所有消费者）。
     */
    default void start() {
    }

    @Override
    default void close() {
        logger().info("Shutting down MQClient [{}]", impl());
    }

    /**
     * 序列化器（从 {@link BaseContext} 查找，由 {@link #init} 注册）。
     */
    default MQEventSerialization serialization() {
        return BaseContext.<String, MQEventSerialization>get(MQ_SERIALIZATION);
    }

    /**
     * MQ 配置（从 {@link BaseContext} 查找）。
     */
    default MQProperties properties() {
        return BaseContext.<String, MQProperties>get(MQEvent.MQ_PROPERTIES);
    }

    /**
     * 消费单个 MQ 事件（核心消费逻辑，由各 {@code initConsumer} 实现内调用）。
     *
     * <p>统一完成：策略匹配 → 租户注入 → 持久化 → 反射调用 {@code @MQEventListener} 方法 → 异常解包。
     * 调用方在 consume 返回或抛异常后，根据 {@link Acknowledgment} 决定 ack/nack/requeue/DLQ。
     *
     * @param listener 监听器定义
     * @param event    已反序列化的 MQ 事件
     * @param ack      确认端口（可为 null，表示该 broker 无 ack 能力）
     * @throws Throwable 业务异常（已解包，调用方据此决定 nack/requeue/DLQ）
     */
    default void consume(MQListener listener, MQEvent event, Acknowledgment ack) throws Throwable {
        if (Objects.isNull(event)) {
            return;
        }
        if (!event.supports(listener.supports())) {
            return;
        }
        logger().info("Consume MQ [{}]: {}", listener.getRouteExpression(this.defaultConcat()), serialization().serialize(event));
        try {
            ThreadContext.set(ContextConstants.TENANT_ID, event.getTenantId());
            // MQ 事件持久化
            MQProperties properties = properties();
            if (Objects.nonNull(properties) && properties.isPersist()) {
                MQEventStorer storer = BaseContext.get(MQ_STORER);
                if (Objects.nonNull(storer)) {
                    try {
                        storer.store(event);
                    } catch (Exception e) {
                        logger().error("Persist MQ failed [{}]: {}", listener.getRouteExpression(this.defaultConcat()),
                                serialization().serialize(event), e);
                    }
                }
            }
            // 反射调用 @MQEventListener 标注的监听方法
            listener.getMethod().invoke(listener.getBean(), event);
        } catch (Exception e) {
            throw unwrap(e);
        } finally {
            ThreadContext.clear();
        }
    }

    // ========================= 物理地址拼接（生产者和消费者侧共享）=========================

    /**
     * 便捷重载：无 Acknowledgment 的消费（ack 能力由 broker 内部处理，如 RocketMQ 返回值语义）。
     */
    default void consume(MQListener listener, MQEvent event) throws Throwable {
        consume(listener, event, null);
    }

    /**
     * 日志器（各实现可覆写自定义 topic）。
     */
    default Logger logger() {
        return LogHolder.logger();
    }

    /**
     * 解析最终的拼接符（concat）。
     *
     * <p>优先级：{@link MQEvent#getConcat()} &gt; 当前 Client 默认值（{@link #defaultConcat()}）。
     * 注：不在 properties 层暴露 concat —— 避免全局配置覆盖 broker 惯例（Kafka 习惯 {@code "_"}、
     * Redis 习惯 {@code ":"}、MQTT 习惯 {@code "/"}）。如需差异化，由各 broker 自己的 Properties 类覆写。
     *
     * @param event MQ 事件（可为 null）
     * @return 三段式拼接符
     */
    default String concat(MQEvent event) {
        if (Objects.nonNull(event) && StrKit.isNotEmpty(event.getConcat())) {
            return event.getConcat();
        }
        return defaultConcat();
    }

    /**
     * Broker 默认拼接符（各 broker 决定，建议子类覆写）。
     *
     * <p>对齐 base-mq {@code MQListener.namespaceTopicTags()}：Rabbit/Rocket/Kafka 用 {@code "."}（Kafka 也常用 {@code "_"}），
     * Redis Stream 用 {@code ":"}，MQTT 用 {@code "/"}。
     *
     * @return broker 默认 concat
     */
    default String defaultConcat() {
        return ".";
    }

    /**
     * 解析命名空间：{@link MQEvent#getNamespace()} 优先，回落到 {@link MQProperties#getNamespace()}。
     *
     * @param event      MQ 事件（可为 null）
     * @param properties MQ 配置（不可为 null）
     * @return 命名空间，可为 null
     */
    default String namespace(MQEvent event, MQProperties properties) {
        if (Objects.nonNull(event) && StrKit.isNotEmpty(event.getNamespace())) {
            return event.getNamespace();
        }
        return namespace((String) null, properties);
    }

    /**
     * 字符串版命名空间解析（{@link MQListener#getNamespace()} 直接传入）。
     *
     * @param namespace  显式命名空间（可为 null）
     * @param properties MQ 配置（可为 null）
     * @return 命名空间，可为 null
     */
    default String namespace(String namespace, MQProperties properties) {
        if (StrKit.isNotEmpty(namespace)) {
            return namespace;
        }
        if (Objects.nonNull(properties)) {
            return properties.getNamespace();
        }
        return null;
    }

    /**
     * 拼接物理地址：{@code namespace[concat]topic[concat]tag}（对齐 base-mq
     * {@code MQListener.namespaceTopicTags()}）。
     *
     * @param namespace 命名空间（可为 null）
     * @param topic     主题
     * @param tag       标签（可为 null）
     * @param concat    拼接符
     * @return 物理地址
     */
    default String resolveTopic(String namespace, String topic, String tag, String concat) {
        String sep = StrKit.isEmpty(concat) ? "." : concat;
        StringBuilder sb = new StringBuilder();
        if (StrKit.isNotEmpty(namespace)) {
            sb.append(namespace).append(sep);
        }
        sb.append(Objects.nonNull(topic) ? topic : "DEFAULT");
        if (StrKit.isNotEmpty(tag)) {
            sb.append(sep).append(tag);
        }
        return sb.toString();
    }

    /**
     * 便捷重载：从 {@link MQEvent} + {@link MQProperties} 解析物理地址（生产者侧）。
     */
    default String resolveTopic(MQEvent event, MQProperties properties) {
        return resolveTopic(namespace(event, properties), event.getTopic(), event.getTag(), concat(event));
    }

    /**
     * 便捷重载：从 {@link MQListener} + {@link MQProperties} 解析物理目的地（消费者侧，
     * tag 取监听器声明的 tags 首个正向 tag，保持订阅定位一致）。
     */
    default String resolveTopic(MQListener listener, MQProperties properties) {
        return resolveTopic(namespace(listener.getNamespace(), properties),
                listener.getTopic(),
                TagMatcher.findIncludes(listener.getTags()).stream().findFirst().orElse(null),
                concat(null));
    }

    /**
     * Listener 端的物理路由键（{@code namespace.topic[.tag]}），
     * 用 {@link #defaultConcat()} 拼接，确保与 producer 端 {@link #resolveTopic(MQEvent, MQProperties)} 同规则。
     *
     * <p>对齐 ddd4j Disruptor 事件的 {@code DisruptorEvent.getRouteExpression()}。
     *
     * @param listener 监听器定义
     * @return 物理路由键
     */
    default String resolveRouteKey(MQListener listener) {
        return listener.getRouteExpression(defaultConcat());
    }

    /**
     * 业务路由 key（partition/sharding/ordered-delivery key）。
     *
     * <p>本质是「业务维度的顺序保证键」——Kafka 用它做 partition 路由（同 key 进同 partition），
     * RabbitMQ 用 JMSXGroupId、Pulsar 用 partition key、RocketMQ 用 messageQueueSelector。
     * 默认实现：{@code tag+"\|"+tenant}（同 tag 同租户顺序保留）。broker 可覆写。
     *
     * <p>子类可通过 {@link PartitionKeyStrategy} 选择内置策略（CUSTOM 时应重写本方法）。
     *
     * @param event MQ 事件（可为 null，null 时返回 null）
     * @return 路由 key，{@code null} 表示不设 key（broker 自由路由）
     */
    default String partitionKey(MQEvent event) {
        if (Objects.isNull(event)) {
            return null;
        }
        String tag = event.getTag();
        String tenant = event.getTenantId();
        if (Objects.nonNull(tag) && Objects.nonNull(tenant)) {
            return tag + "|" + tenant;
        }
        return Objects.nonNull(tag) ? tag : tenant;
    }

    /**
     * tag 消息头的 key（producer 写入 + consumer 读取 + selector 引用）统一用同一个常量。
     *
     * <p>默认 {@code "ddd4jTag"}（无 {@code .}，保证是合法 SQL-92 identifier，
     * JMS Message Selector 可直接当 property 名用）。broker 可覆写。
     *
     * <p>应用层读 header 仍可读 {@link io.ddd4j.mq.message.MessageHeaders#HEADER_DESTINATION_TAG}
     * 作为兼容，但 selector 必须用此 key。
     */
    default String tagHeaderKey() {
        return "ddd4jTag";
    }

    /**
     * 将 {@link MQEventListener#tags()} 表达式翻译为 broker 端 selector（SQL-92 子集），
     * 让订阅时直接传给 broker 做 broker 端过滤（不投递到 listener）。适用于
     * ActiveMQ Artemis、RocketMQ、Kafka 等支持 JMS Message Selector / 订阅 selector 的 broker。
     *
     * <p>对应规则：
     * <ul>
     *   <li>{@code "*"} / {@code null} / 空 → {@code null}（不过滤，所有都投递给 listener）</li>
     *   <li>{@code "paid"} → {@code "tag = 'paid'"}（精确匹配）</li>
     *   <li>{@code "paid || shipped"} → {@code "(tag = 'paid' OR tag = 'shipped')"}</li>
     *   <li>{@code "* -cancelled"} → {@code "(tag <> 'cancelled' OR tag IS NULL)"}
     *       （= 全部但排除 cancelled，无 tag 也允许）</li>
     *   <li>{@code "paid -cancelled"} → {@code "(tag = 'paid' AND (tag <> 'cancelled' OR tag IS NULL))"}
     *       （= paid 或无 tag，但不包含 cancelled）</li>
     * </ul>
     *
     * <p>property name 通过 {@link #tagHeaderKey()} 获取，SQL 字符串字面量用 {@code '} 包。
     *
     * @param tags 监听器声明的 tag 表达式（{@code null} / 空 / {@code "*"} → 不过滤）
     * @return JMS selector 字符串，{@code null} 表示不过滤（由调用方决定是用 broker 还是 fallback 应用层）
     */
    default String tagsToSelector(String tags) {
        if (StrKit.isBlank(tags)) {
            return null;
        }
        // 解析 includes/excludes（与 TagMatcher 语义对齐：* 是通配符，不是字面量）
        Set<String> includes = new LinkedHashSet<>();
        boolean wildcard = false;
        Set<String> excludes = new LinkedHashSet<>();
        for (String token : StrKit.isBlank(tags) ? new String[0]
                : tags.replace("||", " ").trim().split("\\s+")) {
            String t = token.trim();
            if (StrKit.isEmpty(t) || "||".equals(t)) {
                continue;
            }
            if ("*".equals(t)) {
                wildcard = true;
            } else if (t.startsWith("-") && t.length() > 1) {
                excludes.add(t.substring(1));
            } else {
                includes.add(t);
            }
        }
        if (includes.isEmpty() && excludes.isEmpty()) {
            return null;
        }
        // "*" + 仅 excludes：includes 视为通配（1=1）
        if (wildcard) {
            includes = new LinkedHashSet<>();
        }
        if (includes.isEmpty() && excludes.isEmpty()) {
            return null;
        }
        String prop = tagHeaderKey();
        StringBuilder sb = new StringBuilder();
        if (!includes.isEmpty()) {
            // includes: tag IN (...) OR tag IS NULL（与 TagMatcher 一致：tag 为空时也算匹配）
            sb.append("(");
            boolean first = true;
            for (String i : includes) {
                if (!first) {
                    sb.append(" OR ");
                }
                sb.append(prop).append(" = '").append(escape(i)).append("'");
                first = false;
            }
            sb.append(" OR ").append(prop).append(" IS NULL)");
        }
        if (!excludes.isEmpty()) {
            StringBuilder exclude = new StringBuilder();
            boolean first = true;
            for (String e : excludes) {
                if (!first) {
                    exclude.append(" AND ");
                }
                exclude.append("(").append(prop).append(" <> '").append(escape(e)).append("' OR ").append(prop).append(" IS NULL)");
                first = false;
            }
            String left = sb.isEmpty() ? "1=1" : sb.toString();
            sb = new StringBuilder();
            sb.append("(").append(left).append(" AND ").append(exclude).append(")");
        }
        return sb.toString();
    }

    /**
     * 是否在 broker 端做 tag 过滤（不是所有 broker 都支持，返回 false 时调用方应 fallback 应用层）。
     *
     * <p>默认 true 表示「我会用 tagsToSelector 传给 broker」。子 broker 可覆写返回 false 强制应用层过滤
     * （如 Redis Stream / MQTT 等无 selector 机制的 broker）。
     */
    default boolean supportsBrokerTagFilter() {
        return true;
    }

    /**
     * 兼容 {@link #logger()} 的共享 SLF4J 日志持有器。
     */
    @Slf4j(topic = "### DDD4J-MQ ###")
    final class LogHolder {

        private LogHolder() {
        }

        private static Logger logger() {
            return log;
        }
    }

    /**
     * Partition/路由策略枚举（broker 可读取此枚举决定 partitionKey 取值）。
     *
     * <p>NONE：不设 key（轮询路由，性能最佳但无顺序保证）。
     * <p>TAG：按 event.tag（同 tag 顺序）。
     * <p>TENANT：按 event.tenantId（同租户顺序）。
     * <p>TAG_TENANT：按 tag+tenant 复合 key（推荐，最常用）。
     * <p>CUSTOM：业务子类覆写 {@link #partitionKey(MQEvent)}，本枚举不适用。
     */
    enum PartitionKeyStrategy {
        NONE, TAG, TENANT, TAG_TENANT, CUSTOM
    }
}
