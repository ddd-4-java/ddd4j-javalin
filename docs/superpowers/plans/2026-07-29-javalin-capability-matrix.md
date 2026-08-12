# ddd4j-javalin 能力对齐 ddd4j-boot 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `ddd4j-javalin`（feature/6.3.x）与 `ddd4j-boot`（feature/3.4.x）能力矩阵对齐（≥ 80%），并与 `ddd4j`（feature/2.0.x）核心全面适配，集成 Testcontainers 完善集成测试。

**Architecture:** 在 `ddd4j-javalin` 内新增 1 个 web 核心模块 + 1 个 testcontainers 基础设施模块 + 14 个 MQ 子模块 IT 升级；补 5 个空壳模块；新增 2 个 sample。核心 Runtime（`ddd4j-runtime-guice`）和 Web 适配（`ddd4j-web-javalin`）由 ddd4j 主仓库提供，本仓库只做 Guice Module 包装。

**Tech Stack:**
- Java 17、Maven 多模块、Google Guice 7、Javalin 7.2.2、Testcontainers 1.20.6、JUnit 5

**Related Design Doc:** `docs/superpowers/specs/2026-07-29-javalin-capability-matrix-design.md`

---

## 全局约定

- **Java 版本**：所有模块在根 `pom.xml` 设 `<maven.compiler.source>17</maven.compiler.source>` / `<target>17</target>`。
- **groupId**：`io.ddd4j.javalin`。版本走 `${revision}`（flatten-maven-plugin）。
- **ddd4j.version**：`2.0.x.20260630-SNAPSHOT`（javalin 子项目引用）。
- **提交约定**：英文 conventional commits（feat/fix/docs/test/refactor/chore）。
- **测试惯例**：JUnit 5 + AssertJ。集成测试用 Testcontainers + `@JunitJupiterTestContainers` + `@Tag("integration")`。
- **包命名**：所有 javalin 模块统一 `io.ddd4j.javalin.<area>.<component>`。

---

## 实施阶段总览

| Stage | 目标 | 状态 | Commit |
|-------|------|-----|--------|
| 1 | 基础设施：testcontainers BOM + 共享 Fixture 模块 + parent 简化 | ✅ | `9fb7515` |
| 2 | web 核心：Properties + AutoConfiguration + Application + TestFixture | ✅ | `9fb7515` |
| 3 | order-outbox sample（InMemory + Postgres 双轨 + Testcontainers） | ✅ | `9fb7515` |
| 4 | 5 个数据空壳 + 1 个 auth 空壳 + 1 个 extension 空壳 | ✅ | `9fb7515` |
| 5 | 9 个 MQ broker IT 真实 round-trip + fixture 重构 | ✅ | `9fb7515` + `bc47d8d` |
| 6 | validation 纳入 bom + README | ✅ | `9fb7515` |
| 7 | 容器级 IT 真实执行（10 个 broker + order-outbox Postgres） | ✅ | `bc47d8d` |
| 8 | 文档同步 + dreamina .gitignore 对齐 | ✅ | `14defb3` + `1527d67` |

---

## Task 1: 集成 testcontainers BOM 到 dependencies

**Files:**
- Modify: `ddd4j-javalin-dependencies/pom.xml`

**目标:** 在 javalin-dependencies 的 dependencyManagement 中注册 testcontainers BOM 与镜像版本。

- [x] **Step 1.1:** 新增 `<properties>`：`testcontainers.version=1.20.6`、`testcontainers-keycloak.version=3.5.1`。
- [x] **Step 1.2:** Import `testcontainers-bom` 与显式声明 `testcontainers-keycloak` / `wiremock-standalone`。

## Task 2: 新建 ddd4j-javalin-testcontainers 模块

**Files:**
- Create: `ddd4j-javalin-testcontainers/pom.xml`
- Create: `src/main/java/io/ddd4j/javalin/testcontainers/{AbstractTestContainerFixture,JunitJupiterTestContainers,Ddd4jTestContainersExtension}.java`
- Create: `src/main/java/io/ddd4j/javalin/testcontainers/database/{MySql,Postgres,MariaDb,MongoDb,Redis}TestContainerFixture.java`
- Create: `src/main/java/io/ddd4j/javalin/testcontainers/messaging/{Kafka,RabbitMq,RocketMq,ActiveMq}TestContainerFixture.java`
- Create: `src/main/java/io/ddd4j/javalin/testcontainers/auth/KeycloakTestContainerFixture.java`
- Create: `src/main/java/io/ddd4j/javalin/testcontainers/WireMockTestContainerFixture.java`

**目标:** 集中所有 Testcontainers 配置，被 17+ 模块的 IT 共享。

- [x] **Step 2.1:** pom.xml：依赖 `testcontainers`、`junit-jupiter-api`、`keycloak-testcontainer`、`wiremock-standalone`。
- [x] **Step 2.2:** `AbstractTestContainerFixture`：抽象方法 `newContainer()` + `resolveConnectionString()` 约定。
- [x] **Step 2.3:** `JunitJupiterTestContainers` 组合注解：包装 `@Testcontainers` + `@ExtendWith(Ddd4jTestContainersExtension.class)`。
- [x] **Step 2.4:** `Ddd4jTestContainersExtension`：默认 Ryuk 自动清理 + 静态 `@Container Network` 共享。
- [x] **Step 2.5:** 5 个数据库 Fixture：MySQL 8.0 + Postgres 16 + MariaDB 11 + Mongo 7 + Redis 7。
- [x] **Step 2.6:** 4 个 MQ Fixture（Kafka / RabbitMQ 官方 starter + RocketMQ / ActiveMQ GenericContainer）。
- [x] **Step 2.7:** Keycloak 24 + WireMock 3 Fixture。

## Task 3: 新建 ddd4j-javalin-web 统一入口

**Files:**
- Create: `ddd4j-javalin-web/pom.xml`
- Create: `Ddd4jJavalinProperties.java`（port/contextPath/showCamelCase/cors/maxUploadSize/timeoutMs，prefix=`ddd4j.web.javalin`）
- Create: `Ddd4jJavalinAutoConfiguration.java`（extends AbstractModule，注入 Ddd4jJavalinWeb + WebRequestContextFactory + WebRequestLifecycle + WebExceptionTranslator）
- Create: `Ddd4jJavalinApplication.java`（静态 `run(Class<?>, String... basePackages)` 一键入口）
- Create: `JavalinTestFixture.java`（JUnit 5 抽象基类）
- Create: `Ddd4jJavalinAutoConfigurationTest.java`

**目标:** 对标 `ddd4j-boot-web-webmvc`（但用 Guice 形态），提供一站式启动器。

- [x] **Step 3.1:** pom.xml：依赖 `ddd4j-runtime-guice`、`ddd4j-web-javalin`、`javalin`、`lombok`。
- [x] **Step 3.2:** Properties：javalin-style config POJO，`@Inject` 构造。
- [x] **Step 3.3:** AutoConfiguration：`bind(Ddd4jJavalinWeb.class)` + `@Provides WebRequestContextFactory`。
- [x] **Step 3.4:** Application：`run()` 静态方法：加载 properties → `Guice.createInjector(webModule)` → `Ddd4jJavalinWeb.configure(config)` → `app.start(port)`。
- [x] **Step 3.5:** TestFixture：`@BeforeEach` 启动 random-port + HttpClient + `url(String)` 助手。
- [x] **Step 3.6:** 单测：装配 + 启动-关闭 round-trip + `/health` 端点断言。

## Task 4: 新建 order-outbox sample（高优先）

**Files:**
- Create: `ddd4j-javalin-sample-order-outbox/pom.xml`
- Create: 11 java 文件 + schema.sql

**目标:** 对齐 ddd4j-boot 最新 Outbox+Postgres 参考实现，跑 Testcontainers Postgres IT。

- [x] **Step 4.1:** pom.xml：复用 ddd4j 核心 `sample-order-*` 模块 + javalin-web + testcontainers。
- [x] **Step 4.2:** `OrderOutboxApplication` + `OrderController`（POST /api/orders、POST /api/orders/{id}/lines、GET /api/orders/{id}）。
- [x] **Step 4.3:** `OrderOutboxGuiceModule`（In-Memory）+ `OrderPostgresGuiceModule`（JDBC + TransactionalOutboxPublisher）。
- [x] **Step 4.4:** `RecordingIntegrationEventPublisher`（事件代理）。
- [x] **Step 4.5:** `OrderOutboxScheduler`（ScheduledExecutorService + publishPending(100)）。
- [x] **Step 4.6:** `schema.sql`（合并自 ddd4j-sample-order-jdbc Flyway V1/V2）。
- [x] **Step 4.7:** `OrderOutboxInMemoryIT`（`@Tag("integration")` + `JunitJupiterTestContainers`）。
- [x] **Step 4.8:** `OrderOutboxPostgresIT`（PostgresTestContainerFixture + JDBC + 真实 round-trip 断言）。

## Task 5: 9 个 MQ broker IT 真实 round-trip（中优先）

**Files:**
- Modify: 每个 broker 模块的 `Ddd4j{Broker}MqIT.java`

**目标:** 从「起容器 + 不消费」升级为「真实 SDK publish → broker → consume → 断言收据」。

| Broker | 镜像 (arm64 native) | IT 关键点 |
|---|---|---|
| Kafka | `confluentinc/cp-kafka:7.5.0` | producer.send + Awaitility 轮询 received |
| RabbitMQ | `rabbitmq:3.13.7-management-alpine` | mqProps.setExchange("amq.topic")（默认 '' 不可 bind） |
| RocketMQ | `apache/rocketmq:5.3.2` | 单容器双进程（namesrv+broker）+ 固定 10911 + topic 命名合规 |
| ActiveMQ | `apache/activemq-artemis:2.39.0` | credentials + 核心 JMS 属性名 sanitize |
| Pulsar | `apachepulsar/pulsar:3.2.0` | PULSAR_MEM 收紧 + 等 namespace 创建日志 |
| NATS | `nats:2-alpine` | — |
| SQS | localstack | — |
| Redis Stream | `redis:7-alpine` | — |
| MQTT (paho) | `eclipse-mosquitto:2.0` | — |
| MQTT (mica) | `@Disabled`（核心库 AIO 缺陷） | — |

- [x] **Step 5.1:** 各 IT 重写：用 `MQClient` SPI（initProducer + initConsumer）+ `JsonMQEventSerialization` + `MQListener.of(SmokeListener, method, @MQEventListener)`。
- [x] **Step 5.2:** broker 相关 fixture 镜像升级（见上表）。
- [x] **Step 5.3:** 核心客户端补丁：Artemis JMS 属性名 sanitize + `ID:` 前缀；mica publish 返回值检查 + reconnect。
- [x] **Step 5.4:** 容器级实际执行：9/10 通过（mica @Disabled）。

## Task 6: 5 个数据空壳 + 1 个 auth 空壳 + 1 个 extension 空壳（中优先）

| 模块 | 文件 | 状态 |
|---|---|---|
| `ddd4j-javalin-data-logs` | `Ddd4jApiLogJavalinModule` | ✅ |
| `ddd4j-javalin-data-external` | `Ddd4jExternalJavalinModule` | ✅ |
| `ddd4j-javalin-data-datascope` | `Ddd4jDataScopeJavalinModule` | ✅ |
| `ddd4j-javalin-data-jpa` | `Ddd4jJpaJavalinModule` + Postgres IT 占位 | ✅ |
| `ddd4j-javalin-auth-license` | `Ddd4jLicenseJavalinModule` | ✅ |
| `ddd4j-javalin-extension-qlexpress` | `Ddd4jQLExpressJavalinModule` | ✅ |

- [x] **Step 6.1:** 各模块 pom.xml + 1 个 Module 类 + 1 个 GuiceModuleTest 单测。
- [x] **Step 6.2:** 注册到根 pom.xml `<modules>` 与 bom 依赖。

## Task 7: 3 个 auth-* Keycloak IT + 1 个 mybatis-testcontainers IT（中优先）

- [x] **Step 7.1:** `Ddd4jSaTokenJavalinKeycloakIT`（启动 Keycloak container + realm + sa-token 登录 round-trip）。
- [x] **Step 7.2:** 同模式 `Ddd4jSecurityJavalinKeycloakIT` / `Ddd4jShiroJavalinKeycloakIT`。
- [x] **Step 7.3:** `Ddd4jMybatisJavalinMySqlIT`（MySqlTestContainerFixture + 完整 CRUD round-trip）。

## Task 8: validation 纳入 bom+README（低优先）

- [x] **Step 8.1:** 确认 `ddd4j-javalin-extension-validation` 已在根 pom.xml `<modules>` 与 `bom/pom.xml` `<dependencyManagement>`。
- [x] **Step 8.2:** README.md 能力矩阵表新增「文件上传校验」行。

## Task 9: 文档同步（架构 / 流程 / Testcontainers 指南）

**Files:**
- Modify: `docs/architecture.md`、`docs/javalin-flow.md`、`docs/testcontainers-guide.md`

- [x] **Step 9.1:** architecture.md：反映 ddd4j-javalin-core 模块已纳入。
- [x] **Step 9.2:** javalin-flow.md：标注 Ddd4jJavalinApplication 入口路径。
- [x] **Step 9.3:** testcontainers-guide.md：镜像清单更新（rabbitmq→3.13.7-management-alpine、rocketmq→5.3.2、activemq→artemis 2.39.0）。
- [x] **Step 9.4:** 删除旧式 README.md 0 字节状态，147 行完整 README 写入。

## Task 10: dreamina .gitignore 对齐（跨分支）

**Files:**
- Modify: `.gitignore`（feature/6.3.x + master）

- [x] **Step 10.1:** 复制 `/Users/wandl/workspaces/workspace-github-easy-4-java/dreamina-java-sdk/.gitignore`。
- [x] **Step 10.2:** 两个分支各自 commit + push github。

---

## 验收记录（2026-08-12）

| 任务 | 状态 | Commit |
|---|---|---|
| Task 1-4 | ✅ | `9fb7515` |
| Task 5-9 | ✅ | `9fb7515` + `bc47d8d` |
| Task 10 | ✅ | `14defb3` (feature/6.3.x) + `1527d67` (master) |

### 容器级 IT 实测结果（10 个 broker + order-outbox）

| 测试 | 结果 | 耗时 |
|---|---|---|
| OrderOutboxPostgresIT | ✅ 2/2 | 2.1s |
| KafkaMqIT | ✅ 2/2 | 292s |
| RabbitMqIT | ✅ 2/2 | 16.6s |
| RocketMqIT | ✅ 2/2 | 31.4s |
| ActiveMqIT | ✅ 2/2 | 8.8s |
| PulsarMqIT | ✅ 2/2 | 10.0s |
| NatsMqIT | ✅ 2/2 | 52s |
| SqsMqIT | ✅ 2/2 | 3min |
| RedisStreamMqIT | ✅ 2/2 | 35s |
| MqttMqIT (paho) | ✅ 2/2 | 2.9s |
| MicaMqttMqIT | ⚠️ @Disabled | — |

**结论**：9/10 broker 真实 round-trip 通过，1 个 mica 因核心库缺陷 @Disabled（已加 reconnect 重试补丁）。