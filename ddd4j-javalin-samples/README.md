# ddd4j-javalin 示例工程

本目录只放 Javalin 运行时关注的示例：编程式路由、Guice 组装、Javalin Web 适配。通用领域模型优先复用 `io.ddd4j:ddd4j-sample-*`，避免在运行时仓库重复建模。

## 示例清单

| 示例                                            | 方向        | 说明 |
|-----------------------------------------------|-----------|------|
| `ddd4j-javalin-sample-rich-model`             | 普通充血模型   | 复用 `ddd4j-sample-rich-model`，通过 Guice 注入 `OrderApplicationService` 与内存 PO 仓储，Javalin 暴露订单 API |
| `ddd4j-javalin-sample-cqrs-person`            | CQRS / ES | 编程式路由 + Guice 命令服务 + 增量投影 |
| `ddd4j-javalin-sample-auth-satoken`           | Auth      | Javalin + Sa-Token 示例（Keycloak IT） |
| `ddd4j-javalin-sample-auth-security`           | Auth      | Javalin + Spring Security 兼容示例（Keycloak IT） |
| `ddd4j-javalin-sample-auth-shiro`             | Auth      | Javalin + Shiro 兼容示例（Keycloak IT） |
| `ddd4j-javalin-sample-order-outbox`           | Outbox    | InMemory + Postgres 双轨；`TransactionalOutboxPublisher` 真实 round-trip（PostgresTestContainerFixture） |
| `ddd4j-javalin-sample-mybatis-testcontainers` | Data      | Testcontainers MySQL + MyBatis-Plus 端到端 CRUD |
| `ddd4j-javalin-sample-mq-disruptor`           | MQ        | 进程内 Disruptor RingBuffer 示例 |
| `ddd4j-javalin-sample-mq-kafka`               | MQ        | Apache Kafka 示例 |
| `ddd4j-javalin-sample-mq-rabbitmq`            | MQ        | RabbitMQ（AMQP 0-9-1）示例 |

> ⚠️ **sample-mq-{disruptor,kafka,rabbitmq} 暂不参与主构建**：三者面向 2.0.x 线 API（`io.ddd4j.core.event` 包）编写，依赖的上游快照（`io.ddd4j:ddd4j-core/mq-*:2.0.x.20260730-SNAPSHOT`、`ddd4j-runtime-guice:2.0.x`）尚未部署到阿里云私仓，当前无法编译。上游构件补发后，将三个模块加入 `ddd4j-javalin-samples/pom.xml` 的 `<modules>` 即可启用。

验证命令：

```bash
mvn -pl ddd4j-javalin-samples/ddd4j-javalin-sample-rich-model -am compile -DskipTests
```
