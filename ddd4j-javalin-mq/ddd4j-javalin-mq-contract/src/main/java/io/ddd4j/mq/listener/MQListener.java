package io.ddd4j.mq.listener;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.util.TagMatcher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * MQ 监听器定义（100% 对齐 base-mq {@code MQListener}）。
 *
 * <p>由框架适配层（如 ddd4j-mq-spring 的 BeanPostProcessor）在容器初始化阶段构建，
 * 供 {@link MQClient#initConsumer} 建立原生消费者，
 * {@link MQClient#consume} 反射调用目标方法。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unchecked")
public class MQListener {

    /**
     * 目标 Bean
     */
    private Object bean;
    /**
     * 监听方法
     */
    private Method method;
    /**
     * 消费者组，默认 {@code ${应用名}_${方法名}}
     */
    private String group;
    /**
     * 命名空间，默认全局 namespace
     */
    private String namespace;
    /**
     * 主题
     */
    private String topic;
    /**
     * 标签表达式（支持 {@code *} / {@code A || B} / {@code * -C}）
     */
    private String tags;
    /**
     * 策略过滤列表，配合 {@link MQEvent#supports(List)}
     */
    private List<String> supports;
    /**
     * namespace/topic/tag 拼接符，为空时由各 broker 决定默认值
     */
    private String separator;

    /**
     * 从注解与方法元数据构建监听器定义。
     */
    public static MQListener of(Object bean, Method method, MQEventListener ann) {
        return MQListener.builder()
                .bean(bean)
                .method(method)
                .group(ann.group())
                .namespace(ann.namespace())
                .topic(ann.topic())
                .tags(ann.tags())
                .supports(Arrays.asList(ann.supports()))
                .separator(ann.separator())
                .build();
    }

    /**
     * 返回监听方法首个参数类型（约定为 {@link MQEvent} 子类），用于反序列化。
     */
    public Class<? extends MQEvent> payloadType() {
        if (Objects.isNull(method) || method.getParameterCount() == 0) {
            return MQEvent.class;
        }
        Class<?> first = method.getParameterTypes()[0];
        return MQEvent.class.isAssignableFrom(first) ? (Class<? extends MQEvent>) first : MQEvent.class;
    }

    /**
     * 返回策略匹配支持列表。
     */
    public List<String> supports() {
        return Objects.isNull(supports) ? List.of() : supports;
    }

    /**
     * 物理路由键：{@code namespace.topic[.tag]}（与 ddd4j Disruptor
     * {@code DisruptorEvent.getRouteExpression()} 统一规则）。
     *
     * <h3>路由模型</h3>
     * <pre>
     *   namespace.topic.tag
     *   └───┬───┘ └─┬─┘ └┬┘
     *    环境隔离  业务分类  细分标签
     * </pre>
     * <ul>
     *   <li>{@code namespace} —— 命名空间，用于多环境 / 多租户隔离</li>
     *   <li>{@code topic} —— 消费线程隔离维度，不同 topic 走不同消费线程池</li>
     *   <li>{@code tag} —— 同 topic 下共享消费线程，做消息过滤</li>
     * </ul>
     *
     * <p>tag 取 {@code tags} 表达式中第一个正向 include（如 {@code "paid || shipped"} → {@code "paid"}），
     * 与 event 端单 tag 对齐。tag 为 null/空/通配 {@code "*"} 时不追加第三段。
     *
     * @param separator 拼接符（由 {@link io.ddd4j.mq.MQClient#defaultConcat()} 传入，确保 listener 与 event 同规则）
     */
    public String getRouteExpression(String separator) {
        String sep = StrKit.isEmpty(separator) ? "." : separator;
        String ns = Objects.isNull(namespace) ? "" : namespace;
        String tp = Objects.isNull(topic) ? "" : topic;
        String base = ns + sep + tp;
        if (StrKit.isEmpty(tags) || "*".equals(tags.trim())) {
            return base;
        }
        String firstTag = TagMatcher.findIncludes(tags).stream().findFirst().orElse(null);
        if (StrKit.isEmpty(firstTag)) {
            return base;
        }
        return base + sep + firstTag;
    }

}
