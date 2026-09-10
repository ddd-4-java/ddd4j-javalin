# ddd4j-javalin 三分支收敛验证报告

更新日期：2026-09-10

规格事实源：`docs/superpowers/specs/2026-09-07-three-branch-convergence-design.md`

## 构建与版本基线

| 分支 | 功能证据提交 | 构建契约 | ddd4j / Javalin |
|---|---|---|---|
| `feature/6.7.x` | `65afac6` | Maven 3.9.16 / POM 4.0.0 / JDK 17 | `1.0.x.20260630-SNAPSHOT` / `6.7.0` |
| `feature/7.1.x` | `496cbd9` | Maven 3.9.16 / POM 4.0.0 / JDK 17 | `2.0.x.20260630-SNAPSHOT` / `7.1.0` |
| `feature/7.2.x` | `05a8977` | Maven 4.0.0-rc-6 / POM 4.1.0 / JDK 21 | `3.0.x.20260630-SNAPSHOT` / `7.2.3` |

`BuildLineContractTest` 同时检查根 POM 工具链属性、Wrapper、Workflow JDK、Maven 模型、聚合元素、
`MAVEN_SETTINGS_XML`、POM-only 清单、测试日志 provider，以及公共 BOM 对运行时依赖管理的导出。

## 本地与容器证据

三条分支均按自身 JDK/Maven 执行最终 `clean verify`：

| 分支 | 总测试 | failure | error | skip |
|---|---:|---:|---:|---:|
| `feature/6.7.x` | 159 | 0 | 0 | 4 |
| `feature/7.1.x` | 137 | 0 | 0 | 4 |
| `feature/7.2.x` | 137 | 0 | 0 | 4 |

覆盖 MySQL `BaseRepositoryImpl`、PostgreSQL JPA、PostgreSQL Outbox、OIDC Keycloak、九个本地 Broker，
以及 RabbitMQ 的 persistent delivery、publisher confirm、mandatory return、持久化失败 NACK/requeue
和恢复后 ACK。OIDC 负责真实 token/HTTP allow-deny；Sa-Token、Security、Shiro 只声明各自的容器 smoke。

四个 skip 是显式治理结果：ONS 与 TDMQ 需要托管服务凭据，Mica MQTT 的两个用例保留上游 AIO 限制。

## 私有 Maven 仓库证据

- ddd4j 1.0/2.0/3.0 的 POM、主 JAR、sources、javadoc 和 SNAPSHOT metadata 均从独立空缓存取得，三个 ZIP 附件完整性校验通过。
- Phase B 后 Javalin Outbox 已成为三线正式门禁，因此旧的 sample-order 不发布策略已失效。五个共享 sample-order JAR及 `ddd4j-samples` 父 POM已在三线发布，并通过空缓存闭包解析。
- 7.2.x 使用新的 Maven 本地仓库完成 dependency tree 和 52 模块完整单元 Reactor，最终 `BUILD SUCCESS`。
- 公共 `ddd4j-javalin-bom` 现直接继承 `ddd4j-javalin-dependencies`；单纯嵌套 import 会被父级依赖管理抢占，直接继承可确保 6.7.x 消费者解析 Javalin 6.7.0，而不再回退到 4.6.8。
- 最终六线均重新执行 clean deploy：ddd4j 1.0/2.0/3.0 与 Javalin 6.7/7.1/7.2 全部 `BUILD SUCCESS`。三个独立空缓存随后消费最终 Javalin BOM/Web，编译并启动随机端口健康测试，三线均返回 HTTP 200；ddd4j-core 与 ddd4j-javalin-web 的 sources/javadoc 也均从私仓取得并通过 ZIP 完整性检查。

## MQ 可靠性审计边界

RabbitMQ 保持 persistent delivery、publisher confirm、mandatory return、NACK/requeue 与恢复后 ACK
的参考证据。Kafka、NATS、Pulsar、Redis Stream、RocketMQ、SQS、MQTT 和 ActiveMQ 已分别采用协议原生
确认与失败恢复语义：broker Future/同步 send、JetStream、negative ACK、PEL、broker retry、
visibility reset、manual ACK 或 session recover。协议不提供统一 DLQ 的适配器不会伪造 RabbitMQ
语义，而是明确交由 broker policy、业务主题或 ddd4j 核心 DEAD 状态机处理。

## 未关闭门禁

GitHub Actions 最新任务仍在执行任何 step 前失败。Check Run annotation 明确为近期账户付款失败，
或 Actions spending limit 需要提高。所有失败 Job 都是 `steps=0`，因此 `MAVEN_SETTINGS_XML` 校验、
Maven 构建和 Testcontainers 均未获得远端执行机会。

ddd4j 3.0.x 已移除 64 个生态 BOM import，改为显式受管坐标，并将 Model 4.1 内部 parent 统一为固定
GAV。Maven 4 debug validation 中 `io.ddd4j` effective-model 汇总为零；残余告警仅来自第三方
SmallRye、Narayana 与 Pulsar POM。Quarkus test/package/augmentation 和 121 模块 clean test 均已验证。

## 当前结论

本地代码、版本矩阵、真实容器行为、空缓存依赖闭包和六线私仓发布已经形成证据。最终规格保持未关闭，
仅等待 GitHub Actions 在解除组织账户门禁后真正执行且全部通过。

## Phase D 当前增量（尚未发布）

`feature/6.7.x` 工作树已经完成 Phase D Tasks 12–16：真实配置加载与启动前校验、统一
`Ddd4jJavalinRuntime`、严格 MQ 初始化与逆序关闭、MyBatis Repository 自动初始化、JPA 工厂所有权、
真实 readiness、生产 CORS allowlist，以及多实例共享 `IdempotencyGuard` 门禁。共享 Guard Provision
失败时也会回滚已创建的核心 Runtime，不遗留全局 SPI。

当前增量在 Corretto 17 / Maven 3.9.16 下完成 52 模块验证：普通 `clean test` 执行 152 项，零
failure/error/skip；完整 `javalin-integration-tests` 执行 188 项，零 failure/error，并保留且仅保留
ONS、TDMQ、Mica MQTT 共 4 个治理 skip。九类本地 MQ IT 已全部改为从 Javalin 生产生命周期启动。

这些 Phase D 结果仍是未提交工作树证据，尚未同步到 `feature/7.1.x`、`feature/7.2.x`，也未执行新的
GitHub Actions 或阿里云 Maven 发布。因此上文旧版本的三线发布证据不能作为 Phase D 发布完成证明；
必须在三线同步、分支正确工具链验证、最终 SHA Actions 和三个独立空缓存消费全部通过后才能关闭计划。

预推送只读审计确认 `origin` 与 `github` 的三个旧分支头一致；所有 Workflow 均引用
`MAVEN_SETTINGS_XML`、使用对应 JDK，且没有 job-level `continue-on-error`。当前唯一仓库内 Workflow
差异是 `feature/7.1.x` 的 `ci.yml` 缺少 `workflow_dispatch`，须在该分支同步 Phase D 时一并修复。

`feature/7.1.x` 已完成本地 Phase D 兼容同步，保留 Maven 3.9.16、POM 4.0.0、JDK 17 与 Javalin 7
`app.unsafe.routes` API，并补齐 `ci.yml` 的 `workflow_dispatch`。九类支持的 broker 均通过独立生产生命
周期门禁；最终完整集成 Reactor 执行 166 项，零 failure/error，且仅有 4 个治理 skip。该增量尚待提交、
双远端推送与 Actions 验证。

`feature/7.2.x` 已完成本地 Phase D 兼容同步，保留 Maven 4.0.0-rc-6、POM 4.1.0、`<subprojects>`、
JDK 21 与 Javalin 7.2.3。九类 broker 独立生命周期门禁全部通过；显式启用单元与 IT 的最终完整
Reactor 执行 166 项，零 failure/error，4 个治理 skip，PostgreSQL Outbox 3 项均实际执行。
