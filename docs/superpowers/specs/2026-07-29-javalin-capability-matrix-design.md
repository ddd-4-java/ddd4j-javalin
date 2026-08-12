# ddd4j-javalin 能力对齐 ddd4j-boot 设计

- **日期**：2026-07-29
- **作者**：ddd4j-javalin 团队（与 Claude Code 协同设计）
- **状态**：已实施（commit `9fb7515` 2026-08-04 + `bc47d8d` 2026-08-05）
- **关联仓库**：`/Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-javalin`（feature/6.3.x）

## 1. 目标与范围

`ddd4j-javalin` 是 ddd4j 核心（feature/2.0.x）的「Guice Runtime + Javalin 7 Web」实现轨。当前（2026-07）状态：

- 35 个子模块（其中 22 个空壳 pom）
- 22 个 Test（13 真实 + 9 mock）
- Testcontainers：完全无
- Web 入口：缺失（全手工 `Javalin.create()`）
- 文档：README.md 0 字节，顶层 parent 1367 行 Spring 遗留
- 与 ddd4j-boot（38 个子模块）能力对齐率 < 50%

**核心目标**：在不对齐 ddd4j 架构范式（Guice + Javalin 编程式路由 + SubjectKit 静态门面）的前提下，复用 ddd4j 核心已有能力，将能力对齐率提升到 ≥ 80%。

**非目标**：
- 不引入 Spring（对齐对象是能力矩阵，不是平移 Spring 代码）。
- 不重写 ddd4j 核心的任何模块。
- 不实现 ddd4j-boot 的 OWASP/Jacoco/Findbugs 质量门禁（属于 ddd4j-boot 自身治理策略）。

## 2. 对齐策略

ddd4j 核心已提供完整的 6 大 Runtime + Web 适配层（`ddd4j-runtime-guice` + `ddd4j-web-javalin`），javalin 子项目只需要做 **Guice Module 包装 + Properties 绑定 + Testcontainers Fixture + Sample 演示**。

按重要性分四档：

| 优先级 | 任务 | 关键产出 |
|---|---|---|
| 高 | order-outbox sample | `ddd4j-javalin-sample-order-outbox`（InMemory + Postgres 双轨） |
| 高 | 新增 testcontainers 基础设施 | `ddd4j-javalin-testcontainers`（5 数据库 + 4 broker + Keycloak + WireMock Fixture） |
| 高 | 新增 web 统一入口 | `ddd4j-javalin-web`（Properties + AutoConfiguration + Application + JavalinTestFixture） |
| 中 | 9 个 MQ broker IT 真实 round-trip | `Ddd4j{Broker}MqIT.publishAndConsumeRoundTrip()` |
| 中 | 5 个 auth-* Keycloak IT | `Ddd4j{SaToken|Security|Shiro}KeycloakIT` |
| 低 | validation 纳入 bom+README | `ddd4j-javalin-extension-validation` 列入 bom |
| 低 | 4 个数据空壳 | `data-logs` / `data-external` / `data-datascope` / `data-jpa` |
| 低 | 1 个 auth 空壳 | `auth-license` |
| 低 | 1 个 extension 空壳 | `extension-qlexpress` |

## 3. 模块拓扑

```
ddd4j-javalin-samples      ← 演示层（6 现有 + 2 新增）
       ↓
ddd4j-javalin-{web,data,auth,mq,extensions}      ← 适配层（Guice Module 包装）
       ↓
ddd4j-javalin-testcontainers                    ← 共享 Fixture（被 17 个模块依赖）
       ↓
io.ddd4j:ddd4j-runtime-guice + ddd4j-web-javalin  ← 核心 Runtime + Web
```

## 4. 关键设计决策

### 4.1 DI 容器
统一 Google Guice（`com.google.inject:guice`）。每个适配模块提供 `AbstractModule` 子类，IoC 入口在 `Ddd4jJavalinApplication.run(...)` 静态方法中。

### 4.2 路由注册
Javalin 7 不支持注解路由（与 Spring MVC 不同），所有路由通过 `app.routes.post("/path", handler)` 编程式注册：
```java
app.unsafe.routes.post("/api/orders", controller::create);
```
handler 接收 `Context` 参数，通过 `ctx.bodyAsClass(Dto.class)` 反序列化 payload。

### 4.3 配置绑定
Properties POJO 集中在 `ddd4j-javalin-*` 模块，前缀命名 `ddd4j.<area>.javalin.*`：
- `ddd4j.web.javalin.port`
- `ddd4j.data.datasource.url`
- `ddd4j.testcontainers.reuse=true`（仅测试）

### 4.4 测试双轨
- **单测**：H2 / Mockito / In-Memory（`mvn test`）
- **集成测试**：Testcontainers 真实容器（`mvn verify -Pjavalin-integration-tests`）

`javalin-integration-tests` profile 激活 `maven-failsafe-plugin` 跑 `*IT.java`；surefire 跳过。

### 4.5 共享 Testcontainers Fixture
统一在 `ddd4j-javalin-testcontainers` 模块提供：
- 基础类：`AbstractTestContainerFixture` / `JunitJupiterTestContainers` / `Ddd4jTestContainersExtension`
- 数据库 Fixture：MySQL / PostgreSQL / MariaDB / MongoDB / Redis
- 消息中间件 Fixture：Kafka / RabbitMQ / RocketMQ / ActiveMQ
- 鉴权/其他：Keycloak / WireMock
- 等待策略：默认 `Wait.forListeningPort()` + 3min 超时

每个模块的 IT 通过 `<scope>test</scope>` 引入，避免污染生产依赖链。

## 5. 与 ddd4j-boot 的能力矩阵对照

| 能力 | ddd4j-boot 对标 | ddd4j-javalin 适配 | 状态 |
|---|---|---|---|
| Web 入口 | `Ddd4jWebMvcAutoConfiguration` | `Ddd4jJavalinApplication.run` | ✅ 已实施 |
| Web 配置 | `Ddd4jWebMvcProperties` | `Ddd4jJavalinProperties` | ✅ 已实施 |
| 测试基类 | `MockMvc` / `WebTestClient` | `JavalinTestFixture` | ✅ 已实施 |
| 14 个 MQ broker | `ddd4j-boot-mq-*` | `ddd4j-javalin-mq-*` + IT round-trip | ✅ 9/10 通过，mica 因核心 AIO 缺陷 @Disabled |
| 3 个 auth | `SaToken/Security/ShiroAutoConfiguration` | `Ddd4j{SaToken|Security|Shiro}JavalinModule` + Keycloak IT | ✅ 已实施 |
| Cache | `Ddd4jCacheAutoConfiguration` | 直接复用 `ddd4j-cache` 的 `CacheKit` | ✅ 已实施 |
| MyBatis-Plus | `Ddd4jMybatisAutoConfiguration` | `Ddd4jMybatisJavalinModule` | ✅ 已实施 |
| 其它 4 数据空壳 | `data-logs/external/datascope/jpa` | 1 Module + 1 单测 | ✅ 已实施 |
| License | `DefaultLicenseAutoConfiguration` | `Ddd4jLicenseJavalinModule` | ✅ 已实施 |
| QLExpress | `Ddd4jQLExpressBootAutoConfiguration` | `Ddd4jQLExpressJavalinModule` | ✅ 已实施 |
| 二维码 | `Ddd4jQrCodeBootAutoConfiguration` | `Ddd4jQrCodeJavalinModule` | ✅ 已实施 |
| 文件上传校验 | javalin 独有 | `ddd4j-javalin-extension-validation` | ✅ 已纳入 bom |

## 6. 风险与对策

| 风险 | 影响 | 对策 |
|---|---|---|
| testcontainers 镜像在 CI 环境拉取失败 | IT 全部失败 | 在 README 标注「IT 需要 Docker daemon」；CI 镜像预拉取清单；`@DisabledIfDockerUnavailable` |
| `Ddd4jJavalinWeb` 用 `unsafe.routes.*` API 内部暴露 | 与 ddd4j-web-javalin 主版本不兼容 | 锁版本 `2.0.x.20260630-SNAPSHOT`；升级需回归测试 |
| Guice AOP 实现 `@ApiOperationLog` 拦截性能差 | 高频接口性能损耗 | 显式标注才拦截；Guice `Matcher` 精准匹配 |
| 删除 3 个 `@Deprecated` Module 影响 sample | sample 启动失败 | 重构 sample 直接继承 `Ddd4jXxxGuiceModule`（核心 Module） |
| testcontainers 镜像版本漂移 | ddd4j-core 与 javalin-core 测试结果不一致 | 全部锁 `1.20.6`；后续跟进升级 |
| arm64 容器兼容（如 `apache/rocketmq:5.1.0` 无 arm64） | macOS arm64 开发者本地 IT 全失败 | 选 arm64 native tag（`apache/rocketmq:5.3.2`） |

## 7. 验收标准

1. `mvn -pl ddd4j-javalin -am test` 全部绿色（≥ 30 个 Test 类）。
2. `mvn -pl ddd4j-javalin -am verify -Pjavalin-integration-tests` 拉起全部 Testcontainers 镜像并通过（≥ 60 个 IT）。
3. 模块对齐率：ddd4j-javalin 子模块与 ddd4j-boot 能力对应 ≥ 80%。
4. README.md 包含：架构图、能力矩阵表、与 ddd4j-boot 对齐状态、Testcontainers 镜像清单。
5. 架构原则不破坏：维持 Guice + Javalin 编程式路由范式；不引入 Spring；保留 `SubjectKit` 静态门面。