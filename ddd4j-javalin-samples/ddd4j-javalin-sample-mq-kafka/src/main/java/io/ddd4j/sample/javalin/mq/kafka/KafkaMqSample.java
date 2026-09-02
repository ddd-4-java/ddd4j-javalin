package io.ddd4j.sample.javalin.mq.kafka;

import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.event.MQEventPublisher;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.sample.javalin.mq.kafka.mq.OrderCreatedMqListener;
import io.ddd4j.sample.javalin.mq.kafka.mq.config.KafkaMqConfig;
import io.ddd4j.sample.javalin.mq.kafka.order.application.OrderApplicationService;
import io.ddd4j.sample.javalin.mq.kafka.spi.AnonymousSubjectProvider;
import io.ddd4j.sample.javalin.mq.kafka.spi.NoOpDomainEventPublisher;
import io.javalin.Javalin;

/**
 * ddd4j + Javalin + Kafka MQ 示例启动类。
 *
 * <h3>演示链路</h3>
 * <ol>
 *   <li>客户端 POST /orders 创建订单</li>
 *   <li>OrderApplicationService 创建 Order 聚合并发布 OrderCreatedEvent</li>
 *   <li>MQEventPublisher（Kafka 实现）将事件投递到 Kafka Broker</li>
 *   <li>Kafka 消费者拉取并调用 @MQEventListener 方法</li>
 * </ol>
 *
 * <h3>前置条件</h3>
 * <p>需要运行中的 Kafka Broker，默认连接 {@code localhost:9092}。
 * 可通过 {@code ddd4j.mq.kafka.bootstrap-servers} 环境变量修改。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class KafkaMqSample {

    public static void main(String[] args) {
        // 1. 创建 Kafka MQ 基础设施
        KafkaMqConfig mqConfig = new KafkaMqConfig();

        // 2. 注入 ddd4j 核心 SPI 到 BaseContext
        BaseContext.inject(SpiKeys.DOMAIN_EVENT_PUBLISHER,
                io.ddd4j.core.ddd.event.DomainEventPublisher.class, new NoOpDomainEventPublisher());
        BaseContext.inject(SpiKeys.MQ_EVENT_PUBLISHER,
                MQEventPublisher.class, mqConfig.mqEventPublisher());
        BaseContext.inject(SpiKeys.SUBJECT_PROVIDER,
                SubjectProvider.class, new AnonymousSubjectProvider());

        // 3. 注册 MQ 消费者（Kafka 消费者组自动管理）
        OrderCreatedMqListener listener = new OrderCreatedMqListener();
        mqConfig.registerListener(listener);

        // 4. 创建应用服务
        OrderApplicationService orderService = new OrderApplicationService(mqConfig.mqEventPublisher());

        // 5. 启动 Javalin 并注册 REST 端点
        Javalin app = Javalin.create();
        app.unsafe.routes.post("/orders", ctx -> {
            var request = ctx.bodyAsClass(CreateOrderRequest.class);
            var order = orderService.createOrder(request.orderNo(), request.buyerId(), request.buyerName());
            ctx.json(order);
        });

        app.start(8080);
        System.out.println("========================================");
        System.out.println(" ddd4j-javalin-sample-mq-kafka started");
        System.out.println(" Kafka MQ 已连接");
        System.out.println(" 试一试: curl -X POST http://localhost:8080/orders \\");
        System.out.println("   -H 'Content-Type: application/json' \\");
        System.out.println("   -d '{\"orderNo\":\"ORD-001\",\"buyerId\":\"B001\",\"buyerName\":\"张三\"}'");
        System.out.println("========================================");
    }

    /**
     * 创建订单请求 DTO。
     */
    public record CreateOrderRequest(String orderNo, String buyerId, String buyerName) {
    }
}
