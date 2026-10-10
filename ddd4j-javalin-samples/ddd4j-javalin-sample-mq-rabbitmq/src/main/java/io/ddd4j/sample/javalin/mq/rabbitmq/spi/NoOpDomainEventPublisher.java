package io.ddd4j.sample.javalin.mq.rabbitmq.spi;

import io.ddd4j.core.ddd.event.DomainEvent;
import io.ddd4j.core.ddd.event.DomainEventPublisher;

import java.util.Collection;

/**
 * 进程内领域事件发布者：No-Op 示例实现（仅打印）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class NoOpDomainEventPublisher implements DomainEventPublisher {

    /**
     * 空实现：忽略事件。
     * @param <T> 类型参数。
     * @param event 事件消息。
     */
    @Override
    public <T> void publish(DomainEvent<T> event) {
        System.out.println("[DomainEvent] " + event.getClass().getSimpleName());
    }

    /**
     * 空实现：忽略事件集合。
     * @param <T> 类型参数。
     * @param events 领域事件集合。
     */
    @Override
    public <T> void publishAll(Collection<DomainEvent<T>> events) {
        if (events != null) {
            events.forEach(this::publish);
        }
    }
}
