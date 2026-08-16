# ddd4j-javalin-sample-mq-rabbitmq

> ddd4j + Javalin + **RabbitMQ (AMQP)** 示例：演示完整"业务发布 DomainEvent → RabbitMQ 投递 → @MQEventListener 消费"链路。

## 特点

- **企业级消息队列**：基于 AMQP 0-9-1 协议，支持 Exchange/Queue/Binding 路由、消息确认、死信队列
- **业务零 MQ 耦合**：业务代码只依赖 `MQEventPublisher` 接口，与 Disruptor / Kafka 示例完全一致
- **完整链路**：`Order.create()` → `OrderCreatedEvent` → `MQEventPublisher.publish()` → RabbitMQ Exchange → Queue → `@MQEventListener`

## 前置条件

```bash
# 启动 RabbitMQ（使用 Docker）
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13.7-management-alpine

# 管理界面：http://localhost:15672（guest/guest）

# 或通过环境变量指定连接参数
export DDD4J_MQ_RABBITMQ_HOST=your-rabbitmq-host
export DDD4J_MQ_RABBITMQ_PORT=5672
export DDD4J_MQ_RABBITMQ_USERNAME=user
export DDD4J_MQ_RABBITMQ_PASSWORD=pass
```

## 运行

```bash
mvn -pl ddd4j-javalin/ddd4j-javalin-samples/ddd4j-javalin-sample-mq-rabbitmq compile exec:java \
    -Dexec.mainClass=io.ddd4j.sample.javalin.mq.rabbitmq.RabbitMqSample
```

## 测试

```bash
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"ORD-001","buyerId":"B001","buyerName":"张三"}'
```

## 切换 MQ

仅需修改 `pom.xml` 中的依赖：

| MQ 类型 | 依赖 artifactId | 外部依赖 |
|---------|-----------------|---------|
| Disruptor | `ddd4j-mq-disruptor` | 无 |
| Kafka | `ddd4j-mq-kafka` | Kafka Broker |
| RabbitMQ（当前） | `ddd4j-mq-rabbitmq` | RabbitMQ Broker |
