# ddd4j-javalin-sample-mq-disruptor

> ddd4j + Javalin + **Disruptor 本地 MQ** 示例：演示完整"业务发布 DomainEvent → MQ 投递 → @MQEventListener 消费"链路。

## 特点

- **零外部依赖**：基于 LMAX Disruptor RingBuffer，纯进程内内存 MQ，无需 Kafka / RabbitMQ
- **业务零 MQ 耦合**：业务代码只依赖 `MQEventPublisher` 接口，切换 MQ 只需替换 pom 依赖
- **完整链路**：`Order.create()` → `OrderCreatedEvent` → `MQEventPublisher.publish()` → RingBuffer → `@MQEventListener`

## 架构

```
┌─────────────────┐     ┌──────────────────┐     ┌───────────────────────┐
│  POST /orders   │────▶│ OrderAppService  │────▶│  MQEventPublisher     │
│  (Javalin 路由) │     │ .createOrder()   │     │  (Disruptor 实现)     │
└─────────────────┘     └──────────────────┘     └───────────┬───────────┘
                                                            │
                                                            ▼
                                                  ┌───────────────────────┐
                                                  │  Disruptor RingBuffer │
                                                  │  (本地内存队列)       │
                                                  └───────────┬───────────┘
                                                            │
                                                            ▼
                                                  ┌───────────────────────┐
                                                  │ DisruptorMQEventDispatcher │
                                                  │ → @MQEventListener    │
                                                  └───────────────────────┘
```

## 运行

```bash
# 在项目根目录下执行
mvn -pl ddd4j-javalin/ddd4j-javalin-samples/ddd4j-javalin-sample-mq-disruptor compile exec:java \
    -Dexec.mainClass=io.ddd4j.sample.javalin.mq.disruptor.DisruptorMqSample
```

## 测试

```bash
# 创建订单（触发 OrderCreatedEvent → MQ 消费）
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"ORD-001","buyerId":"B001","buyerName":"张三"}'
```

## 切换 MQ

仅需修改 `pom.xml` 中的依赖：

| MQ 类型 | 依赖 artifactId | 外部依赖 |
|---------|-----------------|---------|
| Disruptor（当前） | `ddd4j-mq-disruptor` | 无 |
| Kafka | `ddd4j-mq-kafka` | Kafka Broker |
| RabbitMQ | `ddd4j-mq-rabbitmq` | RabbitMQ Broker |

业务代码、`@MQEventListener` 注解、`MQEventPublisher` 调用方式 **完全无需修改**。
