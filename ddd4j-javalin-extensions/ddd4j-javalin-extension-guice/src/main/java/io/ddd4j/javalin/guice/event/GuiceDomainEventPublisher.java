package io.ddd4j.javalin.guice.event;

import com.google.common.eventbus.EventBus;
import io.ddd4j.core.contract.DomainEvent;
import io.ddd4j.core.contract.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Guice 实现的领域事件发布者
 * <p>
 * 使用 Guava EventBus 机制发布领域事件，
 * 替代 Spring 的 ApplicationEventPublisher。
 *
 * @author Loong Wan
 */
public class GuiceDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(GuiceDomainEventPublisher.class);

    private final EventBus eventBus;

    public GuiceDomainEventPublisher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            logger.warn("Attempted to publish null domain event");
            return;
        }
        logger.debug("Publishing domain event: {}, aggregateId: {}", event.getEventType(), event.getAggregateId());
        eventBus.post(event);
    }

    @Override
    public void publishAll(Collection<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        logger.debug("Publishing {} domain events", events.size());
        for (DomainEvent event : events) {
            publish(event);
        }
    }
}
