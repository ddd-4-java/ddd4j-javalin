package io.ddd4j.sample.javalin.mq.disruptor.mq.config;

import io.ddd4j.mq.disruptor.config.DisruptorMQProperties;
import io.ddd4j.mq.disruptor.core.DisruptorMQBus;
import io.ddd4j.mq.disruptor.core.DisruptorMQEventDispatcher;
import io.ddd4j.mq.disruptor.publisher.DisruptorMQEventPublisher;

/**
 * Disruptor 本地 MQ 配置：手动组装 Disruptor 组件。
 *
 * <p>Javalin 无 DI 容器（Spring/CDI），因此需要手动创建以下组件：
 * <ul>
 *   <li>{@link DisruptorMQProperties}：RingBuffer 配置</li>
 *   <li>{@link DisruptorMQEventDispatcher}：事件分发器（按 routeKey 路由到消费者）</li>
 *   <li>{@link DisruptorMQBus}：RingBuffer 生命周期管理</li>
 *   <li>{@link DisruptorMQEventPublisher}：事件发布实现（注入 BaseContext）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class DisruptorMqConfig {

    private final DisruptorMQProperties properties;
    private final DisruptorMQEventDispatcher dispatcher;
    private final DisruptorMQBus bus;
    private final DisruptorMQEventPublisher publisher;

    /**
     * 构造并启动 Disruptor MQ。
     */
    public DisruptorMqConfig() {
        // RingBuffer 配置：默认 1024 槽位、YieldingWaitStrategy
        this.properties = new DisruptorMQProperties();
        properties.setBufferSize(1024);
        properties.setWaitStrategy("yielding");
        properties.setNamespace("javalin-disruptor-sample");
        properties.setDefaultTopic("DEFAULT");

        // 事件分发器
        this.dispatcher = new DisruptorMQEventDispatcher();

        // 启动 Disruptor（内部创建 RingBuffer 并绑定 dispatcher）
        this.bus = new DisruptorMQBus(properties, dispatcher);

        // 事件发布者
        this.publisher = new DisruptorMQEventPublisher(bus, properties);
    }

    /**
     * 返回事件发布者（注入 BaseContext 或业务服务使用）。
     */
    public DisruptorMQEventPublisher mqEventPublisher() {
        return publisher;
    }

    /**
     * 返回事件分发器（注册 MQListener 用）。
     */
    public DisruptorMQEventDispatcher dispatcher() {
        return dispatcher;
    }

    /**
     * 返回 Disruptor MQ 总线（生命周期管理）。
     */
    public DisruptorMQBus bus() {
        return bus;
    }
}
