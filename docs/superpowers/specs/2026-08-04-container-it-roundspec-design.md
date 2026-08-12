# ddd4j-javalin 容器级 IT 真实 round-trip 修复设计

- **日期**：2026-08-04
- **状态**：已实施（commit `bc47d8d`）
- **目标**：在 `9fb7515` 提交 IT 代码编写完成的基础上，**真实拉起容器**运行 9 个 MQ broker IT 与 order-outbox IT，publish → broker → consume 全链路断言通过。

## 1. 问题

`9fb7515` 的 IT 编写完成并保证 `mvn test`（单元测试）全绿，但**所有 `*IT.java` 仅编译通过**——从未在容器中实际执行。容错、镜像兼容、等待策略、broker 配置等运行时问题需要真实环境验证。

## 2. 真实执行结果（2026-08-04 至 2026-08-05）

| 测试 | 结果 | 耗时 | 关键问题 |
|---|---|---|---|
| OrderOutboxPostgresIT | ✅ 2/2 | 2.1s | 1. m2 旧 jar（rollback 字段）；2. 核心 order-jdbc 重装后通过 |
| KafkaMqIT | ✅ 2/2 | 292s | — |
| RabbitMqIT | ❌ → ✅ 2/2 | 16.6s | 1. arm64 `badmap` 崩溃；2. 默认 `""` exchange 不可 bind；3. wait strategy 超时 |
| RocketMqIT | ❌ → ✅ 2/2 | 31.4s | 1. 5.1.0 无 arm64 QEMU OOM；2. broker.conf brokerIP1 配置；3. 10909 无 proxy 不监听；4. testcontainers-bom 管 remoting 5.2.0 缺 API；5. topic `.` 非法字符；6. IT 覆盖了 fixture 10911 绑定 |
| ActiveMqIT | ❌ → ✅ 2/2 | 8.8s | 1. classic 5.x 与 Artemis 客户端协议不兼容；2. JMS 属性名 `.`/`-` 非法；3. JMSMessageID 必须 `ID:` 前缀 |
| PulsarMqIT | ❌ → ✅ 2/2 | 10.0s | 1. standalone OOM；2. namespace 晚于端口监听；3. opentelemetry-api 1.25→1.54.1 |
| NatsMqIT | ✅ 2/2 | 52s | — |
| SqsMqIT | ✅ 2/2 | 3min | localstack |
| RedisStreamMqIT | ✅ 2/2 | 35s | — |
| MqttMqIT (paho) | ✅ 2/2 | 2.9s | — |
| MqttMicaMqIT | ⚠️ `@Disabled` | — | mica-mqtt 2.6.6 AIO 在 mac arm64 上首连被拒、`publish()` 恒返回 true |

**总计**：10 个 broker 中 **9 个真实 round-trip 通过**；mica 因核心库缺陷跳过。

## 3. 关键修复（按 broker）

### 3.1 RabbitMQ
- **镜像**：`rabbitmq:3-management` → `rabbitmq:3.13.7-management-alpine`（arm64 兼容）。
- **exchange**：IT 显式 `mqProps.setExchange("amq.topic")`（默认 `""` 为 default exchange，不可 `queueBind`，消息无队列可投）。

### 3.2 RocketMQ
- **镜像**：`apache/rocketmq:5.1.0` → `5.3.2`（arm64 native）。
- **单容器双进程**：5.3.x 镜像 entrypoint 只启动单进程，fixture 用 `withCommand` 覆盖为 `sh mqnamesrv & sleep 10; sh mqbroker -n 127.0.0.1:9876 -c broker.conf & wait`。
- **brokerIP1**：通过挂载 `broker.conf`（含 `brokerIP1=127.0.0.1`）确保 broker 注册到 localhost。
- **固定 10911**：用 `FixedHostPortGenericContainer`（broker 注册 127.0.0.1:10911）。
- **JVM 堆**：`JAVA_OPT_EXT=-Xmx512m -Xms512m -Xmn128m`（默认 2g OOMKilled）。
- **只暴露 9876**：10909 是 proxy VIP 端口，无 proxy 时 broker 不监听，暴露会导致 `Wait.forListeningPort` 永远超时。
- **依赖对齐**：pom 显式补 `rocketmq-remoting:5.5.0`（testcontainers-bom 管 5.2.0 缺 `setScanAvailableNameSrv`）。
- **topic 命名**：IT TOPIC 改 `ddd4j_it_rocket`（`.` 在 RocketMQ 中非法）。
- **fixture 单一权威**：IT 删除自身的 `withCommand`/`withCreateContainerCmdModifier`（覆盖了 fixture 的固定 10911 绑定）。

### 3.3 ActiveMQ
- **镜像**：`apache/activemq-classic:5.18.3` → `apache/activemq-artemis:2.39.0`（artemis 镜像被 Artemis 客户端识别）。
- **凭证**：`artemis/artemis` 默认凭证。
- **核心补丁**（在 ddd4j 主仓库提交，本仓库通过 m2 jar 生效）：
  - `ActiveMQClient.jmsProperty(name)` —— JMS 属性名 sanitize（`.`/`-` → `_`）。
  - `setJMSMessageID("ID:" + msgId)` —— Artemis 严格要求前缀。
  - `ActiveMQAcknowledgment` import 由 `javax.jms` 迁移 `jakarta.jms`。

### 3.4 Pulsar
- **PULSAR_MEM**：`PULSAR_MEM="-Xms512m -Xmx512m -XX:MaxDirectMemorySize=1g"`（standalone 默认 OOMKilled）。
- **等待策略**：`Wait.forLogMessage(".*Created namespace public/default.*")`（端口监听早于 namespace 初始化）。
- **opentelemetry-api**：pom 显式补 1.54.1（旧 BOM 管 1.25，缺 `DoubleHistogramBuilder.setExplicitBucketBoundariesAdvice`）。

### 3.5 mica（唯一未通过）
- **核心补丁**：在 ddd4j 主仓库 `MicaMqttMQClient` 加入 publish 返回值检查 + reconnect 重试。
- **结论**：AIO 首连被拒、`publish()` 恒返回 true（入队而非确认发送）——核心库 `mica-mqtt 2.6.6` 在 mac arm64 上的缺陷，javalin 适配层无法修复。
- **现状**：IT 标 `@Disabled("mica-mqtt AIO 客户端在 macOS arm64 上 publish 静默丢失（核心库缺陷）")`，待上游修复后移除。

## 4. 验证

```bash
mvn verify -Pjavalin-integration-tests -pl ddd4j-javalin-mq/ddd4j-javalin-mq-{broker} -am
```

Docker daemon 必须可用（Docker Desktop 已启动）。容器级 IT 与单测 **互不干扰**：单测 `mvn test` 跳过 `*IT.java`，IT `mvn verify -P...` 才执行。

## 5. 关联

- `docs/superpowers/specs/2026-07-29-javalin-capability-matrix-design.md`
- `docs/superpowers/plans/2026-07-29-javalin-capability-matrix.md` Task 5（部分修订）