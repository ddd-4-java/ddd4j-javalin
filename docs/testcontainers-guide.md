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
| `rabbitmq:3-management` | `org.testcontainers:rabbitmq` | MQ |
| `apache/activemq-classic:5.18.3` | GenericContainer | MQ |
| `apache/rocketmq:5.1.0` | GenericContainer | MQ |
| `apachepulsar/pulsar:3.2.0` | GenericContainer | MQ |
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
| ActiveMQ | `Wait.forListeningPort()` |
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