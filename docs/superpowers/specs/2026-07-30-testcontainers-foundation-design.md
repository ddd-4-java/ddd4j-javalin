# ddd4j-javalin Testcontainers 基础设施设计

- **日期**：2026-07-30
- **状态**：已实施（commit `9fb7515`）
- **目标**：在 ddd4j-javalin 子项目内建立统一 Testcontainers Fixture 共享层，供 17+ 模块的 IT 共用。

## 1. 动机

ddd4j-javalin 当前（2026-07）IT 数量 22 个，其中 13 个为 mock，9 个虽然用了真实 broker 但只断言「容器启动成功」而非真实 round-trip。**核心痛点**：

1. 每个模块的 IT 都要自己写 `new GenericContainer<>("postgres:16")...` 等样板；
2. broker 真实消费未覆盖，无法保证对接 SDK 的正确性；
3. docker compose / 测试脚本零散，缺少统一启动约定。

## 2. 设计

### 2.1 模块结构

新建独立模块 `ddd4j-javalin-testcontainers`，产出 jar 仅含测试基础设施：

```
ddd4j-javalin-testcontainers
├── pom.xml                             # 仅 test scope 依赖
└── src/main/java/io/ddd4j/javalin/testcontainers/
    ├── AbstractTestContainerFixture.java   # 抽象 fixture 基类
    ├── JunitJupiterTestContainers.java      # 组合注解
    ├── Ddd4jTestContainersExtension.java    # 共享 Network + Ryuk 自动清理
    ├── WireMockTestContainerFixture.java
    ├── database/
    │   ├── MySqlTestContainerFixture.java
    │   ├── PostgresTestContainerFixture.java
    │   ├── MariaDbTestContainerFixture.java
    │   ├── MongoDbTestContainerFixture.java
    │   └── RedisTestContainerFixture.java
    ├── messaging/
    │   ├── KafkaTestContainerFixture.java
    │   ├── RabbitMqTestContainerFixture.java
    │   ├── RocketMqTestContainerFixture.java
    │   └── ActiveMqTestContainerFixture.java
    └── auth/
        └── KeycloakTestContainerFixture.java
```

### 2.2 Fixture 契约

```java
public abstract class AbstractTestContainerFixture<C extends GenericContainer<?>> {
    public abstract C newContainer();              // 未启动容器
    public String connectionString() { ... }       // 短生命周期：start → 读 host/port → stop
    protected abstract String resolveConnectionString(C container);
}
```

测试典型用法：
```java
@Tag("integration")
@JunitJupiterTestContainers
class MyFeatureIT {
    @Container static final MySQLContainer<?> MYSQL = new MySqlTestContainerFixture().newContainer();
    @Test void test() {
        MYSQL.start();
        // 用 MYSQL.getJdbcUrl() 做断言
    }
}
```

### 2.3 镜像版本（与 testcontainers-bom 1.20.6 对齐）

| 镜像 | 初始版本 | 后续修正 |
|---|---|---|
| `mysql:8.0` | ✅ | — |
| `postgres:16-alpine` | ✅ | — |
| `mariadb:11` | ✅ | — |
| `mongo:7` | ✅ | — |
| `redis:7-alpine` | ✅ | — |
| `confluentinc/cp-kafka:7.5.0` | ✅ | — |
| `rabbitmq:3-management` | ⚠️ arm64 上 badmap 崩溃 → **`3.13.7-management-alpine`** | `bc47d8d` |
| `apache/activemq-classic:5.18.3` | ⚠️ 客户端是 Artemis 6.x → **`apache/activemq-artemis:2.39.0`** | `bc47d8d` |
| `apache/rocketmq:5.1.0` | ⚠️ 无 arm64、QEMU OOMKilled → **`5.3.2`**（arm64 native） + 收紧堆 | `bc47d8d` |
| `apachepulsar/pulsar:3.2.0` | ⚠️ standalone OOMKilled → **PULSAR_MEM 收紧 + namespace log wait** | `bc47d8d` |
| `nats:2-alpine` | ✅ | — |
| `eclipse-mosquitto:2.0` | ✅ | — |

### 2.4 等待策略

- 默认 `Wait.forListeningPort()` + 3 分钟启动超时。
- RocketMQ / Pulsar / RabbitMQ 等需用 `Wait.forLogMessage(...)` 等日志匹配（standalone 容器端口早于 namespace 初始化）。

## 3. 验证

- 模块自身：`FixtureContractTest`（11 个 fixture 全部能构造容器对象，不需要启动）。
- 17+ 依赖模块通过 fixture 复用消除各自重复的容器配置代码。

## 4. 关联

- `docs/superpowers/specs/2026-07-29-javalin-capability-matrix-design.md`（主设计）
- `docs/superpowers/plans/2026-07-29-javalin-capability-matrix.md` Task 2、5