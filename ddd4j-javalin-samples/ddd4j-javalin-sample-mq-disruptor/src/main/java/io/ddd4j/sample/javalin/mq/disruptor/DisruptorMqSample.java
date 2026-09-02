package io.ddd4j.sample.javalin.mq.disruptor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.contract.MQEventPublisher;
import io.ddd4j.core.i18n.I18nProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.sample.javalin.mq.disruptor.mq.OrderCreatedMqListener;
import io.ddd4j.sample.javalin.mq.disruptor.mq.config.DisruptorMqConfig;
import io.ddd4j.sample.javalin.mq.disruptor.spi.AnonymousSubjectProvider;
import io.ddd4j.sample.javalin.mq.disruptor.spi.NoOpDomainEventPublisher;
import io.javalin.Javalin;

/**
 * ddd4j + Javalin + Disruptor 本地 MQ 示例启动类。
 *
 * <h3>演示链路</h3>
 * <ol>
 *   <li>客户端 POST /orders 创建订单</li>
 *   <li>OrderApplicationService 创建 Order 聚合并发布 OrderCreatedEvent</li>
 *   <li>MQEventPublisher（Disruptor 实现）将事件序列化投递到 RingBuffer</li>
 *   <li>DisruptorMQEventDispatcher 按 topic+tag 匹配并调用 @MQEventListener 方法</li>
 *   <li>OrderCreatedMqListener 消费事件并记录日志</li>
 * </ol>
 *
 * <h3>核心要点</h3>
 * <p>Javalin 无 DI 容器，因此：
 * <ul>
 *   <li>通过 Guice 组装 MQ 基础设施（DisruptorMQBus / DisruptorMQEventPublisher）</li>
 *   <li>将 MQEventPublisher 注入到 BaseContext，供业务代码透明使用</li>
 *   <li>手动注册 MQListener 到 DisruptorMQEventDispatcher</li>
 * </ul>
 *
 * <h3>运行</h3>
 * <pre>{@code
 * mvn -pl ddd4j-javalin/ddd4j-javalin-samples/ddd4j-javalin-sample-mq-disruptor compile exec:java \
 *     -Dexec.mainClass=io.ddd4j.sample.javalin.mq.disruptor.DisruptorMqSample
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class DisruptorMqSample {

    public static void main(String[] args) {
        // 1. 创建 Disruptor MQ 基础设施
        DisruptorMqConfig mqConfig = new DisruptorMqConfig();

        // 2. 注入 ddd4j 核心 SPI 到 BaseContext
        BaseContext.inject(SpiKeys.DOMAIN_EVENT_PUBLISHER,
                io.ddd4j.core.ddd.event.DomainEventPublisher.class, new NoOpDomainEventPublisher());
        BaseContext.inject(SpiKeys.MQ_EVENT_PUBLISHER,
                MQEventPublisher.class, mqConfig.mqEventPublisher());
        BaseContext.inject(SpiKeys.SUBJECT_PROVIDER,
                SubjectProvider.class, new AnonymousSubjectProvider());

        // 3. 创建 Guice 注入器，组装业务 Bean
        Injector injector = Guice.createInjector(binder -> {
            // 绑定 MQ 基础设施
            binder.bind(MQEventPublisher.class).toInstance(mqConfig.mqEventPublisher());
            // 绑定领域事件发布者（本示例使用 NoOp，实际应用应桥接到 MQEventPublisher）
            binder.bind(io.ddd4j.core.ddd.event.DomainEventPublisher.class)
                    .toInstance(new NoOpDomainEventPublisher());
        });

        // 4. 注册 MQ 消费者到 Disruptor 分发器
        OrderCreatedMqListener listener = new OrderCreatedMqListener();
        mqConfig.dispatcher().register(
                io.ddd4j.mq.registry.MQListenerDefinition.builder()
                        .bean(listener)
                        .method(resolveOnOrderCreated(listener))
                        .group("javalin-disruptor-sample_onOrderCreated")
                        .namespace("")
                        .topic("ORDER")
                        .tags("CREATED")
                        .build(),
                message -> {
                    // 委托给 @MQEventListener 标注的方法
                    try {
                        Object payload = io.ddd4j.core.utils.JsonKit.fromJson(
                                (String) message.getPayload(),
                                io.ddd4j.sample.javalin.mq.disruptor.order.domain.OrderCreatedEvent.class);
                        listener.onOrderCreated((io.ddd4j.sample.javalin.mq.disruptor.order.domain.OrderCreatedEvent) payload);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        // 5. 启动 Javalin 并注册 REST 端点
        Javalin app = Javalin.create();
        app.post("/orders", ctx -> {
            var request = ctx.bodyAsClass(CreateOrderRequest.class);
            var service = new io.ddd4j.sample.javalin.mq.disruptor.order.application.OrderApplicationService(
                    mqConfig.mqEventPublisher());
            var order = service.createOrder(request.orderNo(), request.buyerId(), request.buyerName());
            ctx.json(order);
        });
        app.get("/orders/{id}", ctx -> {
            ctx.json(java.util.Map.of("message", "订单查询端点（本示例仅演示 MQ 发布/消费链路）"));
        });

        app.start(8080);
        System.out.println("========================================");
        System.out.println(" ddd4j-javalin-sample-mq-disruptor started");
        System.out.println(" Disruptor 本地 MQ 已就绪（无外部依赖）");
        System.out.println(" 试一试: curl -X POST http://localhost:8080/orders \\");
        System.out.println("   -H 'Content-Type: application/json' \\");
        System.out.println("   -d '{\"orderNo\":\"ORD-001\",\"buyerId\":\"B001\",\"buyerName\":\"张三\"}'");
        System.out.println("========================================");
    }

    /**
     * 反射获取 onOrderCreated 方法。
     */
    private static java.lang.reflect.Method resolveOnOrderCreated(OrderCreatedMqListener listener) {
        try {
            return listener.getClass().getMethod("onOrderCreated",
                    io.ddd4j.sample.javalin.mq.disruptor.order.domain.OrderCreatedEvent.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建订单请求 DTO。
     */
    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }
}
