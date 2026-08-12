# Testcontainers 集成测试指南

## 镜像清单

来自 https://testcontainers.com/modules/ 官方支持 Java 的镜像(锁版本 `1.20.6`)。

| 镜像 | artifactId | 用途 |
|---|---|---|
| `mysql:8.0` | `org.testcontainers:mysql` | RDBMS |
| `postgres:16-alpine` | `org.testcontainers:postgresql` | RDBMS |
| `mariadb:11` | `org.testcontainers:mariadb` | RDBMS |
| `mongo:7` | `org.testcontainers:mongodb` | NoSQL |
| `redis:7-alpine` | `org.testcontainers:testcontainers` (GenericContainer) | NoSQL / Cache |
| `confluentinc/cp-kafka:7.5.0` | `org.testcontainers:kafka` | MQ |
| `rabbitmq:3.13.7-management-alpine` | `org.testcontainers:rabbitmq` | MQ（arm64 native） |
| `apache/activemq-artemis:2.39.0` | GenericContainer | MQ（dd4j-mq-activemq 客户端是 Artemis 6.x） |
| `apache/rocketmq:5.3.2` | GenericContainer | MQ（arm64 native + 单容器双进程模式） |
| `apachepulsar/pulsar:3.2.0` | GenericContainer | MQ（standalone + PULSAR_MEM 收紧） |
| `nats:2-alpine` | GenericContainer | MQ |
| `eclipse-mosquitto:2.0` | GenericContainer | MQ |
| `localstack/localstack:3.4` | GenericContainer | AWS 仿真(SQS 等) |
| `quay.io/keycloak/keycloak:24.0` | `com.github.dasniko:testcontainers-keycloak` | Auth(OIDC) |
| `wiremock/wiremock:3.5.0` | `org.wiremock:wiremock-standalone` | HTTP Mock |

## 等待策略速查

| 容器 | 等待策略 |
|---|---|
| MySQL / Postgres / MariaDB | 默认 JDBC SELECT 1 |
| MongoDB | 默认 MongoDB isreplicaset 探测 |
| Redis | `Wait.forListeningPort()` + `redis-cli ping` 验证 |
| Kafka | 默认 Confluent KRaft 启动日志 + AdminClient metadata |
| RabbitMQ | 默认 management API 健康检查 |
| ActiveMQ（Artemis）| `Wait.forListeningPort()` + 3min 启动超时 |
| RocketMQ（5.3.x 单容器双进程）| `Wait.forListeningPort()` 仅监听 9876（10909 无 proxy 不监听，10911 固定映射由 fixture 处理） |
| Pulsar | `Wait.forLogMessage(".*Created namespace public/default.*")`（端口早于 namespace 初始化） |
| Keycloak | `Wait.forHttp("/health/ready")` |
| WireMock | 默认 stub 加载完成 |

## 写 IT 的标准模式

```java
@Tag("integration")
@JunitJupiterTestContainers
class MyFeatureIT extends JavalinTestFixture {

    @Container
    static MySQLContainer<?> MYSQL = new MySqlTestContainerFixture().newContainer();

    @BeforeAll
    static void setupSchema() throws Exception {
        MYSQL.start();
        try (Connection c = MYSQL.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE ...");
        }
    }

    @AfterAll
    static void teardown() { MYSQL.stop(); }

    @Test void shouldRoundTrip() throws Exception {
        HttpResponse<String> r = http(HttpRequest.newBuilder(url("/api/foo")).GET().build());
        assertThat(r.statusCode()).isEqualTo(200);
    }
}
```

## 跑 IT

```bash
mvn -Pjavalin-integration-tests verify
```

未启用 profile 时 surefire 排除 `*IT.java`,failsafe 默认不启动 → 单测无需 Docker 即可跑。

## 容器级 IT 真实执行（2026-08-04 至 2026-08-05）

通过 `mvn -Pjavalin-integration-tests verify` 在 Docker daemon 环境下拉起容器并跑 publish→consume round-trip：

| Broker | 状态 | 关键修复 |
|---|---|---|
| Kafka | ✅ | — |
| RabbitMQ | ✅ | arm64 image 替换（3-management → 3.13.7-management-alpine）+ `amq.topic` exchange 显式指定 |
| RocketMQ | ✅ | 5.1.0→5.3.2（arm64）+ 单容器双进程（fixture withCommand）+ brokerIP1=127.0.0.1 + 固定 10911 + 收紧 JVM 堆 + 仅暴露 9876（10909 无 proxy 不监听） |
| ActiveMQ（Artemis）| ✅ | classic→artemis 镜像 + 核心 JMS 属性名 sanitize（`.`/`-`→`_`）+ JMSMessageID `ID:` 前缀 |
| Pulsar | ✅ | PULSAR_MEM 收紧 + 等 namespace 创建日志 + otel-api 1.54.1 |
| NATS / SQS / Redis Stream / MQTT(paho) | ✅ | — |
| MQTT(mica) | ⚠️ `@Disabled` | mica-mqtt 2.6.6 AIO 在 mac arm64 上首连被拒 + publish 恒返回 true（核心库缺陷）|

设计细节与失败回放：参见 [`docs/superpowers/specs/2026-08-04-container-it-roundspec-design.md`](../superpowers/specs/2026-08-04-container-it-roundspec-design.md)。