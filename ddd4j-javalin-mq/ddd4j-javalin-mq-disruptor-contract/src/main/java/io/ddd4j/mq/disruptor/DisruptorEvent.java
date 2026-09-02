package io.ddd4j.mq.disruptor;

import io.ddd4j.kit.lang.StrKit;
import lombok.Getter;
import lombok.Setter;

/**
 * LMAX RingBuffer 中承载的 ddd4j 消息事件。
 *
 * <p>该事件模型由 ddd4j 自己维护，确保本地消息模块不依赖未发布的扩展库。</p>
 */
@Getter
@Setter
public class DisruptorEvent {

    private String namespace;
    private String topic;
    private String tag;
    private String messageId;
    private Object payload;
    private long sequence;

    public String getRouteExpression() {
        StringBuilder route = new StringBuilder();
        if (StrKit.isNotBlank(namespace)) {
            route.append(namespace).append('.');
        }
        route.append(topic);
        if (StrKit.isNotBlank(tag)) {
            route.append('.').append(tag);
        }
        return route.toString();
    }
}
