package io.ddd4j.sample.javalin.mq.kafka.spi;

import io.ddd4j.core.ddd.event.DomainEvent;
import io.ddd4j.core.ddd.event.DomainEventPublisher;

import java.util.Collection;

/**
 * 进程内领域事件发布者：No-Op 示例实现（仅打印）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class NoOpDomainEventPublisher implements DomainEventPublisher {

    @Override
    public <T> void publish(DomainEvent<T> event) {
        System.out.println("[DomainEvent] " + event.getClass().getSimpleName());
    }

    @Override
    public <T> void publishAll(Collection<DomainEvent<T>> events) {
        if (events != null) {
            events.forEach(this::publish);
        }
    }
}
