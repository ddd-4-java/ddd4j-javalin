package io.ddd4j.guice.event;

/**
 * 任意对象事件发布能力（1.0.x 改挂补钉）。
 *
 * <p>2.0.x 的 {@code DomainEventPublisher} 内置 {@code default void publish(Object)}，
 * 供非 {@code DomainEvent} 体系的事件（如 Web 请求失败事件）路由到本地事件总线；
 * 1.0.x 的 {@code DomainEventPublisher} 仅保留 {@code publish(DomainEvent)}。
 * 本接口在 javalin 侧恢复该契约：实现方（如 {@link GuiceDomainEventPublisher}）
 * 将任意事件对象路由到 Guava EventBus。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 6.7.x
 */
public interface ObjectEventPublisher {

    /**
     * 发布任意事件对象（非 {@code DomainEvent} 体系）。
     *
     * @param event 任意事件对象
     */
    void publishObject(Object event);
}
