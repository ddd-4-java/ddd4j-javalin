package io.ddd4j.sample.javalin.mq.rabbitmq;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.contract.MQEventPublisher;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.sample.javalin.mq.rabbitmq.mq.OrderCreatedMqListener;
import io.ddd4j.sample.javalin.mq.rabbitmq.mq.config.RabbitMqConfig;
import io.ddd4j.sample.javalin.mq.rabbitmq.order.application.OrderApplicationService;
import io.ddd4j.sample.javalin.mq.rabbitmq.spi.AnonymousSubjectProvider;
import io.ddd4j.sample.javalin.mq.rabbitmq.spi.NoOpDomainEventPublisher;
import io.javalin.Javalin;

/**
 * ddd4j + Javalin + RabbitMQ (AMQP) 示例启动类。
 *
 * <h3>演示链路</h3>
 * <ol>
 *   <li>客户端 POST /orders 创建订单</li>
 *   <li>OrderApplicationService 创建 Order 聚合并发布 OrderCreatedEvent</li>
 *   <li>MQEventPublisher（RabbitMQ 实现）将事件投递到 RabbitMQ Exchange</li>
 *   <li>RabbitMQ 按 routingKey 路由到 Queue，消费者调用 @MQEventListener 方法</li>
 * </ol>
 *
 * <h3>前置条件</h3>
 * <p>需要运行中的 RabbitMQ Broker，默认连接 {@code localhost:5672}（guest/guest）。
 * 可通过环境变量 {@code DDD4J_MQ_RABBITMQ_HOST} / {@code DDD4J_MQ_RABBITMQ_PORT} 修改。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class RabbitMqSample {

    public static void main(String[] args) {
        // 1. 创建 RabbitMQ MQ 基础设施
        RabbitMqConfig mqConfig = new RabbitMqConfig();

        // 2. 注入 ddd4j 核心 SPI 到 BaseContext
        BaseContext.inject(SpiKeys.DOMAIN_EVENT_PUBLISHER,
                io.ddd4j.core.ddd.event.DomainEventPublisher.class, new NoOpDomainEventPublisher());
        BaseContext.inject(SpiKeys.MQ_EVENT_PUBLISHER,
                MQEventPublisher.class, mqConfig.mqEventPublisher());
        BaseContext.inject(SpiKeys.SUBJECT_PROVIDER,
                SubjectProvider.class, new AnonymousSubjectProvider());

        // 3. 注册 MQ 消费者
        OrderCreatedMqListener listener = new OrderCreatedMqListener();
        mqConfig.registerListener(listener);

        // 4. 创建应用服务
        OrderApplicationService orderService = new OrderApplicationService(mqConfig.mqEventPublisher());

        // 5. 启动 Javalin 并注册 REST 端点
        Javalin app = Javalin.create();
        app.post("/orders", ctx -> {
            var request = ctx.bodyAsClass(CreateOrderRequest.class);
            var order = orderService.createOrder(request.orderNo(), request.buyerId(), request.buyerName());
            ctx.json(order);
        });

        app.start(8080);
        System.out.println("========================================");
        System.out.println(" ddd4j-javalin-sample-mq-rabbitmq started");
        System.out.println(" RabbitMQ (AMQP) 已连接");
        System.out.println(" 试一试: curl -X POST http://localhost:8080/orders \\");
        System.out.println("   -H 'Content-Type: application/json' \\");
        System.out.println("   -d '{\"orderNo\":\"ORD-001\",\"buyerId\":\"B001\",\"buyerName\":\"张三\"}'");
        System.out.println("========================================");
    }

    /**
     * 创建订单请求 DTO。
     */
    public final static class CreateOrderRequest {

        private static final long serialVersionUID = 0L;

        private final String orderNo;

        private final String buyerId;

        private final String buyerName;

        @JsonCreator()
        public CreateOrderRequest(@JsonProperty("orderNo") String orderNo, @JsonProperty("buyerId") String buyerId, @JsonProperty("buyerName") String buyerName) {
            this.orderNo = orderNo;
            this.buyerId = buyerId;
            this.buyerName = buyerName;
        }

        @JsonProperty("orderNo")
        public String orderNo() {
            return orderNo;
        }

        @JsonProperty("buyerId")
        public String buyerId() {
            return buyerId;
        }

        @JsonProperty("buyerName")
        public String buyerName() {
            return buyerName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            CreateOrderRequest other = (CreateOrderRequest) obj;
            return Objects.equals(this.orderNo, other.orderNo) && Objects.equals(this.buyerId, other.buyerId) && Objects.equals(this.buyerName, other.buyerName);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(orderNo);
            result = 31 * result + Objects.hashCode(buyerId);
            result = 31 * result + Objects.hashCode(buyerName);
            return result;
        }

        @Override
        public String toString() {
            return "CreateOrderRequest[orderNo=" + orderNo + ", buyerId=" + buyerId + ", buyerName=" + buyerName + "]";
        }
    }
}
