# Testcontainers 集成测试指南

## 依赖与分类

本项目锁定 Testcontainers Java `1.20.6`。Testcontainers 模块目录同时列出官方模块和社区模块；Docker 镜像本身也不等同于
Testcontainers 官方模块。

| 服务 / 镜像（钉版） | Java 适配方式 | 分类 / 出处 |
|---|---|---|
| MySQL `mysql:8.0.46` | `org.testcontainers:mysql` / `MySQLContainer` | Testcontainers 官方模块 <https://testcontainers.com/modules/mysql/> |
| PostgreSQL `postgres:16.15-alpine` | `org.testcontainers:postgresql` / `PostgreSQLContainer` | Testcontainers 官方模块 <https://testcontainers.com/modules/postgresql/> |
| MariaDB `mariadb:11.8.9` | `org.testcontainers:mariadb` / `MariaDBContainer` | Testcontainers 官方模块 <https://testcontainers.com/modules/mariadb/> |
| MongoDB `mongo:7.0.43` | `org.testcontainers:mongodb` / `MongoDBContainer` | Testcontainers 官方模块 <https://testcontainers.com/modules/mongodb/> |
| Kafka `confluentinc/cp-kafka:7.5.0` | `org.testcontainers:kafka` / `KafkaContainer` | Testcontainers 官方模块 <https://testcontainers.com/modules/kafka/> |
| RabbitMQ `rabbitmq:3.13.7-management-alpine` | `org.testcontainers:rabbitmq` / `RabbitMQContainer` | Testcontainers 官方模块 <https://testcontainers.com/modules/rabbitmq/> |
| LocalStack `localstack/localstack:3.4.0` | `org.testcontainers:localstack` / `LocalStackContainer` | Testcontainers 官方模块 <https://testcontainers.com/modules/localstack/> |
| ActiveMQ Artemis `apache/activemq-artemis:2.39.0` | `org.testcontainers:activemq` / `ArtemisContainer` | Testcontainers 官方模块 <https://testcontainers.com/modules/artemis/> |
| Pulsar `apachepulsar/pulsar:3.2.0` | `org.testcontainers:pulsar` / `PulsarContainer` | Testcontainers 官方模块 <https://testcontainers.com/modules/pulsar/> |
| Keycloak `quay.io/keycloak/keycloak:26.2.5` | `com.github.dasniko:testcontainers-keycloak:3.7.0` | 社区模块；对应 TC 1.20.6 <https://testcontainers.com/modules/keycloak/> |
| Redis `redis:7.4.11-alpine` | `GenericContainer` | 无 org.testcontainers 第一方工件，保留通用容器 <https://testcontainers.com/modules/redis/> |
| RocketMQ `apache/rocketmq:5.3.2` | `GenericContainer` | 模块目录无 rocketmq 页，按 Docker Hub 官方镜像页核对 <https://hub.docker.com/r/apache/rocketmq> |
| NATS `nats:2.15.0-alpine` | `GenericContainer` | 无 org.testcontainers 第一方工件（社区在 io.github.amadeusitgroup） <https://testcontainers.com/modules/nats/> |
| MQTT `eclipse-mosquitto:2.0.22` | `GenericContainer` | 模块目录列出但无 org.testcontainers Java 工件，保留通用容器 <https://testcontainers.com/modules/mosquitto/> |
| WireMock `wiremock/wiremock:3.5.4` | WireMock 容器 fixture（`GenericContainer`）+ standalone 客户端 | 官方镜像 <https://testcontainers.com/modules/wiremock/> |

镜像标签由集中式 fixture 固定（含补丁级钉版，原浮动别名如 `8.0`/`16-alpine`/`11`/`7`/`3.4`/`26.2`/`2-alpine`/`2.0` 已改为固定补丁标签，钉版前后 digest 一致），禁止 `latest`。版本选型参考 [Testcontainers 模块目录](https://testcontainers.com/modules/) 和 [Keycloak 社区模块版本矩阵](https://github.com/dasniko/testcontainers-keycloak/blob/main/docs/versions.md)。

## 等待与验收策略

| 容器                         | 启动与验收                                                                           |
|------------------------------|--------------------------------------------------------------------------------------|
| MySQL / PostgreSQL / MariaDB | JDBC 可用后执行真实写入、读取或事务断言                                              |
| MongoDB                      | 默认副本集探测后执行读写断言                                                         |
| Redis                        | 监听端口就绪后执行 stream round-trip                                                 |
| Kafka / RabbitMQ             | 通过 `JavalinMqLifecycleParticipant` 初始化并执行 publish → consume → acknowledgment |
| ActiveMQ Artemis             | 监听端口就绪后执行 JMS round-trip                                                    |
| RocketMQ                     | nameserver 9876 就绪；fixture 管理固定 10911 映射，然后执行消息 round-trip           |
| Pulsar                       | 等待默认 namespace 创建完成，再执行消息 round-trip                                   |
| Keycloak                     | 健康端点就绪并导入 `ddd4j-test` realm，随后验证 token 和 allow/deny                  |

## 运行方式

默认单元测试不要求 Docker：

```bash
./mvnw clean test
```

执行全部集成测试：

```bash
./mvnw -Pjavalin-integration-tests verify
```

排障或避免固定端口服务互相影响时，应按模块串行执行 IT：

```bash
./mvnw -Pjavalin-integration-tests \
  -pl ddd4j-javalin-mq/ddd4j-javalin-mq-kafka -am \
  -Dit.test=Ddd4jKafkaMqIT \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dfailsafe.failIfNoSpecifiedTests=false verify
```

Kafka、RabbitMQ、NATS、Pulsar、ActiveMQ、RocketMQ、Redis Stream、MQTT 与 SQS 的 round-trip
必须从生产 MQ 生命周期启动，不允许在 IT 中直接调用 `MQClient.init(...)`。MySQL Repository 与
PostgreSQL JPA IT 同样通过 `Ddd4jJavalinRuntime` 初始化和关闭资源。普通 `./mvnw clean test` 必须执行
单元测试；根 POM 禁止设置 `maven-surefire-plugin` 的 `skip=true` 或 `skipTests=true`。

本地无法复现的托管服务 ONS、TDMQ 明确禁用；Mica MQTT 因已记录的上游 AIO 缺陷明确禁用。禁用原因必须留在对应测试上，不能用
workflow 级 `continue-on-error` 代替。
