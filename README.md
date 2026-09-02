# ddd4j-javalin

> 基于 [Javalin 7](https://javalin.io/) + [Google Guice](https://github.com/google/guice) 的 [ddd4j](https://github.com/ddd4j/ddd4j) 框架适配层。
>
> 对齐目标:与 [`ddd4j-boot`](../ddd4j-boot) 能力矩阵一致,与 [`ddd4j`](../ddd4j) 全面适配。

## 项目定位

ddd4j 是面向 DDD + CQRS + Event Sourcing 的服务端基础库,提供 6 大 Runtime 适配(Spring / Quarkus / Micronaut / Helidon / Dropwizard / Guice)。
ddd4j-javalin 是其中"Guice Runtime + Javalin 7 Web"的实现轨,目标是为不使用 Spring 生态、又追求轻量的 Java 服务端项目提供与 Spring Boot 同等的"开箱即用"体验。

## 核心设计原则

1. **不引入 Spring** —— DI 容器统一为 Google Guice。
2. **不引入注解路由** —— Javalin 7 不支持注解路由,所有路由通过 `app.routes.post(...)` 编程式注册。
3. **静态门面优先** —— `SubjectKit.login() / getSubject() / hasPermission()` 代替框架 API。
4. **ddd4j 核心 100% 复用** —— javalin 适配层只是 Guice Module 包装,不重新造底层。
5. **测试双轨** —— 单元测试用 H2 / Mockito / In-Memory;集成测试用 Testcontainers。

## 能力矩阵(对齐 ddd4j-boot 状态)

| 能力 | ddd4j-boot 对标 | ddd4j-javalin 模块 | 状态 |
|---|---|---|---|
| **Web 启动入口** | `ddd4j-boot-web-webmvc` | `ddd4j-javalin-web` (`Ddd4jJavalinApplication.run`) | ✅ |
| **Web 配置** | `Ddd4jWebMvcProperties` | `Ddd4jJavalinProperties` (`ddd4j.web.javalin.*`) | ✅ |
| **Javalin 测试基类** | `MockMvc` / `WebTestClient` | `JavalinTestFixture` (`app.start(0)` + HttpClient) | ✅ |
| **Cache** | `Ddd4jCacheAutoConfiguration` | 直接复用 `ddd4j-cache` 的 `CacheKit` | ✅ |
| **MyBatis-Plus** | `Ddd4jMybatisAutoConfiguration` | `Ddd4jMybatisJavalinModule` (H2 + MySQL IT) | ✅ |
| **API 操作日志** | `Ddd4jApiLogAspectAutoConfiguration` | `Ddd4jApiLogJavalinModule` | ✅ |
| **外部数据源(IP/天气)** | `Ddd4jExternalAutoConfiguration` | `Ddd4jExternalJavalinModule` | ✅ |
| **数据权限** | `Ddd4jDataScopeAutoConfiguration` | `Ddd4jDataScopeJavalinModule` | ✅ |
| **JPA/Hibernate** | `ddd4j-boot-data-jpa` | `Ddd4jJpaJavalinModule` (PostgreSQL IT 占位) | ✅ |
| **License** | `DefaultLicenseAutoConfiguration` | `Ddd4jLicenseJavalinModule` | ✅ |
| **Sa-Token** | `SaTokenEnhanceAutoConfiguration` | `Ddd4jSaTokenJavalinModule` (Keycloak IT) | ✅ |
| **Spring Security** | `SecurityEnhanceAutoConfiguration` | `Ddd4jSecurityJavalinModule` (Keycloak IT) | ✅ |
| **Apache Shiro** | `ShiroEnhanceAutoConfiguration` | `Ddd4jShiroJavalinModule` (Keycloak IT) | ✅ |
| **MQ 14 broker** | `ddd4j-boot-mq-*` | `ddd4j-javalin-mq-*` (Testcontainers IT) | ✅ |
| **二维码** | `Ddd4jQrCodeBootAutoConfiguration` | `Ddd4jQrCodeJavalinModule` | ✅ |
| **QLExpress 规则** | `Ddd4jQLExpressBootAutoConfiguration` | `Ddd4jQLExpressJavalinModule` | ✅ |
| **文件上传校验（Validator）** | javalin 独有 | `ddd4j-javalin-extension-validation` | ✅ |
| **akka / excel / jackson / monitor / pf4j** | 各 `Ddd4j*BootAutoConfiguration` | 占位 pom | ⚠️ 待补 |

## Testcontainers 集成测试镜像

| 类别 | 镜像 | Module |
|---|---|---|
| RDBMS | `mysql:8.0` / `postgres:16-alpine` / `mariadb:11` | `ddd4j-javalin-testcontainers` |
| NoSQL | `mongo:7` / `redis:7-alpine` | `ddd4j-javalin-testcontainers` |
| MQ | `confluentinc/cp-kafka:7.5.0` / `rabbitmq:3.13.7-management-alpine` / `apache/activemq-artemis:2.39.0` / `apache/rocketmq:5.3.2` / `apachepulsar/pulsar:3.2.0` / `nats:2-alpine` / `eclipse-mosquitto:2.0` / `localstack/localstack:3.4` | `ddd4j-javalin-testcontainers` |
| Auth | `quay.io/keycloak/keycloak:24.0` | `ddd4j-javalin-testcontainers` |
| HTTP Mock | `wiremock/wiremock:3.5.0` | `ddd4j-javalin-testcontainers` |

## 跑集成测试

```bash
# 仅单元测试(默认 profile,无需 Docker daemon)
mvn test

# 集成测试(需要 Docker daemon,启动真容器)
mvn -Pjavalin-integration-tests verify
```

## 模块结构

```
ddd4j-javalin/
├── ddd4j-javalin-bom                    # 版本 BOM
├── ddd4j-javalin-dependencies           # 第三方依赖管理
├── ddd4j-javalin-ddd                    # DDD 基础入口(fuinorg + ddd4j-core)
├── ddd4j-javalin-web                    # Web 启动入口(★新增)
├── ddd4j-javalin-testcontainers         # Testcontainers 基础设施(★新增)
├── ddd4j-javalin-cache                  # 缓存桥接(CacheKit)
├── ddd4j-javalin-data/                  # 数据层
│   ├── ddd4j-javalin-data-mybatisplus
│   ├── ddd4j-javalin-data-jpa           # ★从空壳补完
│   ├── ddd4j-javalin-data-panache
│   ├── ddd4j-javalin-data-crypto
│   ├── ddd4j-javalin-data-logs          # ★从空壳补完
│   ├── ddd4j-javalin-data-external      # ★从空壳补完
│   └── ddd4j-javalin-data-datascope     # ★从空壳补完
├── ddd4j-javalin-auth/                  # 鉴权层
│   ├── ddd4j-javalin-auth-license       # ★从空壳补完
│   ├── ddd4j-javalin-auth-satoken
│   ├── ddd4j-javalin-auth-security
│   └── ddd4j-javalin-auth-shiro
├── ddd4j-javalin-mq/                    # 14 个 MQ broker
│   ├── ddd4j-javalin-mq-core
│   ├── ddd4j-javalin-mq-{activemq,disruptor,kafka,rabbitmq,rocketmq,pulsar,nats,mqtt,mqtt-mica,sqs,redis-stream,ons,tdmq}
├── ddd4j-javalin-extensions/            # 扩展模块
│   ├── ddd4j-javalin-extension-qrcode
│   ├── ddd4j-javalin-extension-qlexpress # ★从空壳补完
│   ├── ddd4j-javalin-extension-validation  # ★文件上传校验
│   └── ddd4j-javalin-extension-{akka,excel,jackson,monitor,pf4j}   # 仍为空
├── ddd4j-javalin-parent                 # Sample parent
└── ddd4j-javalin-samples/               # 示例
    ├── ddd4j-javalin-sample-rich-model              # ★升级:用 Ddd4jJavalinApplication
    ├── ddd4j-javalin-sample-order-outbox           # ★新增:InMemory + Postgres 双轨
    ├── ddd4j-javalin-sample-mybatis-testcontainers  # ★新增:Testcontainers 端到端
    ├── ddd4j-javalin-sample-auth-{satoken,security,shiro}
    ├── ddd4j-javalin-sample-cqrs-person
    └── ddd4j-javalin-sample-mq-{disruptor,kafka,rabbitmq}
```

## 一键启动(Sample 6.3.x)

```java
public class MyApplication {
    public static void main(String[] args) {
        Ddd4jJavalinApplication.run(
            args,                              // 标准 CLI 参数: --port 9090 或裸端口
            "io.example.myapp",                // DDD 注解扫描 base package
            new MyGuiceModule(),               // 业务 Module
            new Ddd4jSaTokenJavalinModule(),   // 鉴权 Module
            new Ddd4jMybatisJavalinModule(ds)  // 数据 Module
        );
    }
}
```

## 一行集成测试

> `JavalinTestFixture` 位于 `io.ddd4j.javalin.testcontainers.web`（`ddd4j-javalin-testcontainers` 模块，test scope 依赖）。

```java
class MyIT extends JavalinTestFixture {
    @Container static MySQLContainer<?> MYSQL = new MySqlTestContainerFixture().newContainer();

    @Override protected String[] basePackages() { return new String[]{"io.example.myapp"}; }

    @Test void shouldGreet() throws Exception {
        HttpResponse<String> r = http(HttpRequest.newBuilder(url("/hello")).GET().build());
        assertThat(r.statusCode()).isEqualTo(200);
    }
}
```

## 历史与版本

- **2.0.x 轨** — 已定型,逐步达到人类架构师产出水准(参见 [`ddd4j`](../ddd4j))
- **6.3.x 轨**(本仓库)— 框架集成层,正在追赶 [`ddd4j-boot`](../ddd4j-boot) 的能力矩阵
- 当前快照:`1.0.x.20260630-SNAPSHOT` / `ddd4j 2.0.x` / Java 17

### 三档版本矩阵（2026-09 定版）

| 档位 | core 上游 | 本仓库分支 | Javalin | Jackson | JDK | 定位 |
|------|----------|-----------|---------|---------|-----|------|
| 低档 | `1.0.x.20260630-SNAPSHOT` | `feature/6.7.x`（改挂工程：`opt/retarget-1.0.x` @ tag `m5-retarget-1.0.x`，142 测试全绿） | 6.7.0 | Jackson 2 | 17* | 存量 Spring 生态维护线 |
| 中档 | `2.0.x.20260730-SNAPSHOT` | `feature/7.1.x` @ c62b7e5 | 7.1.0 | Jackson 2 | 17 | **新项目主力线**（JDK17+J2 当前主流基线） |
| 高档 | `3.0.x.20260630-SNAPSHOT` | `feature/7.2.x` @ f5cf1b1 | 7.2.3 | Jackson 3 | 21 | 前瞻预留线（未来升 Javalin 7.3.x） |

\* 低档 javalin 侧编译 Java 17，消费的 core 1.0.x 构件为 Java 8。

中档/高档共性排除清单（上游构件在对应 core 线不存在）：extension-akka/jackson/pf4j、sample-cqrs-person/rich-model；低档另排除 extension-akka/jackson/pf4j（同因）。auth-security 链在低档暂排（待上游 1.0.x 修复 core.auth.* 面板）。

## 生产就绪状态

当前阶段：**internal beta**（功能完成、56 模块单测全绿、10 个容器级 IT 中 9 个真实 round-trip 通过）。

2026-08-16 实施了三项生产就绪改进：

| 改进项 | 说明 | 状态 |
|--------|------|------|
| **CI workflows** | GitHub Actions 自动化 build + test + quality | 已实施（待推送验证） |
| **Quality profile** | Jacoco 覆盖率（骨架就位，agent 注入点待修）+ OWASP 漏洞扫描 + SpotBugs 手动分析 | 部分实施 |
| **Release profile + 文档** | GPG 签名发布配置 + 发布工程演练手册 | 已实施 |

**剩余阻塞项**（首个正式版发布前必须解决）：
1. 核心依赖 SNAPSHOT 收敛：`ddd4j-parent:2.0.x.*` 需先发布正式版
2. 核心仓库远端分叉裁决：`feature/1.0.x` 的 javax/jakarta 方向冲突待用户决策

详见 [RELEASE.md](RELEASE.md) 和 [生产就绪设计文档](docs/superpowers/specs/2026-08-16-production-readiness-design.md)。

## 详细文档

- [docs/architecture.md](docs/architecture.md) — 架构与模块关系
- [docs/javalin-version-matrix.md](docs/javalin-version-matrix.md) — Javalin 版本 × JDK 基线矩阵（选型依据与维护规则）
- [docs/javalin-flow.md](docs/javalin-flow.md) — 请求生命周期
- [docs/testcontainers-guide.md](docs/testcontainers-guide.md) — 集成测试镜像与等待策略

## 规划与历史（superpowers）

按 SDD（Spec-Driven Development）流程，所有新需求的设计、计划、状态报告统一在 `docs/superpowers/`：

- `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md` —— 设计文档
- `docs/superpowers/plans/YYYY-MM-DD-<topic>.md` —— 实施计划（checkbox 跟踪）
- `docs/superpowers/reports/YYYY-MM-DD-status.md` —— 真实状态报告

主要历史：
- [`specs/2026-07-29-javalin-capability-matrix-design.md`](docs/superpowers/specs/2026-07-29-javalin-capability-matrix-design.md) — 能力矩阵对齐设计
- [`plans/2026-07-29-javalin-capability-matrix.md`](docs/superpowers/plans/2026-07-29-javalin-capability-matrix.md) — 实施计划（4 阶段 + 容器级 IT 验收）
- [`specs/2026-07-30-testcontainers-foundation-design.md`](docs/superpowers/specs/2026-07-30-testcontainers-foundation-design.md) — Testcontainers Fixture 设计
- [`specs/2026-08-04-container-it-roundspec-design.md`](docs/superpowers/specs/2026-08-04-container-it-roundspec-design.md) — 容器级 IT 真实 round-trip 修复设计
- [`reports/2026-08-05-status.md`](docs/superpowers/reports/2026-08-05-status.md) — 当前真实状态

## 许可证

Apache 2.0