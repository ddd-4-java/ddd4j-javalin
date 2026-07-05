# ddd4j-javalin-sample-mq-kafka

> ddd4j + Javalin + **Kafka MQ** 示例：演示完整"业务发布 DomainEvent → Kafka 投递 → @MQEventListener 消费"链路。

## 特点

- **分布式消息队列**：基于 Apache Kafka，支持高吞吐、持久化、消费者组
- **业务零 MQ 耦合**：业务代码只依赖 `MQEventPublisher` 接口，与 Disruptor 示例完全一致
- **完整链路**：`Order.create()` → `OrderCreatedEvent` → `MQEventPublisher.publish()` → Kafka Topic → `@MQEventListener`

## 前置条件

```bash
# 启动 Kafka（使用 Docker）
docker run -d --name kafka -p 9092:9092 apache/kafka:latest

# 或通过环境变量指定 Broker 地址
export DDD4J_MQ_KAFKA_BOOTSTRAP_SERVERS=your-kafka-broker:9092
```

## 运行

```bash
mvn -pl ddd4j-javalin/ddd4j-javalin-samples/ddd4j-javalin-sample-mq-kafka compile exec:java \
    -Dexec.mainClass=io.ddd4j.sample.javalin.mq.kafka.KafkaMqSample
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
| Kafka（当前） | `ddd4j-mq-kafka` | Kafka Broker |
| RabbitMQ | `ddd4j-mq-rabbitmq` | RabbitMQ Broker |
