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
| `feature/6.7.x` | 158 | 0 | 0 | 4 |
| `feature/7.1.x` | 136 | 0 | 0 | 4 |
| `feature/7.2.x` | 131 | 0 | 0 | 4 |

覆盖 MySQL `BaseRepositoryImpl`、PostgreSQL JPA、PostgreSQL Outbox、OIDC Keycloak、九个本地 Broker，
以及 RabbitMQ 的 persistent delivery、publisher confirm、mandatory return、持久化失败 NACK/requeue
和恢复后 ACK。OIDC 负责真实 token/HTTP allow-deny；Sa-Token、Security、Shiro 只声明各自的容器 smoke。

四个 skip 是显式治理结果：ONS 与 TDMQ 需要托管服务凭据，Mica MQTT 的两个用例保留上游 AIO 限制。

## 私有 Maven 仓库证据

- ddd4j 1.0/2.0/3.0 的 POM、主 JAR、sources、javadoc 和 SNAPSHOT metadata 均从独立空缓存取得，三个 ZIP 附件完整性校验通过。
- Phase B 后 Javalin Outbox 已成为三线正式门禁，因此旧的 sample-order 不发布策略已失效。五个共享 sample-order JAR及 `ddd4j-samples` 父 POM已在三线发布，并通过空缓存闭包解析。
- 7.2.x 使用新的 Maven 本地仓库完成 dependency tree 和 52 模块完整单元 Reactor，最终 `BUILD SUCCESS`。
- 公共 `ddd4j-javalin-bom` 现显式 import `ddd4j-javalin-dependencies`，避免 6.7.x 消费者回退到上游管理的 Javalin 4.6.8。

## MQ 可靠性审计边界

RabbitMQ 已达到协议级持久化与恢复证据。其他 Broker 的正常 publish/consume 与 ACK 映射已有测试；
NATS 和 Kafka 另有真实上游集成测试。ActiveMQ、Pulsar、SQS、RocketMQ 等仍以各自协议的确认映射和
Javalin 真实往返为主，不能表述为已经完成与 RabbitMQ 相同的故障注入、宕机恢复和 DLQ 验收。

## 未关闭门禁

GitHub Actions 最新任务仍在执行任何 step 前失败。Check Run annotation 明确为近期账户付款失败，
或 Actions spending limit 需要提高。所有失败 Job 都是 `steps=0`，因此 `MAVEN_SETTINGS_XML` 校验、
Maven 构建和 Testcontainers 均未获得远端执行机会。

Maven 4 仍会报告 ddd4j 3.0.x 聚合平台中的 BOM import 冲突。ddd4j 已使用精确 allowlist 和最终版本
权威校验约其结果，但“警告为零”尚未实现，需要拆分当前一次导入 64 个生态 BOM 的
`ddd4j-dependencies`，不能在 Javalin 层通过隐藏日志解决。

## 当前结论

本地代码、版本矩阵、真实容器行为、空缓存依赖闭包和上游私仓构件已经形成证据。最终规格保持未关闭，
直到最新提交完成私仓发布，并且 GitHub Actions 在解除账户门禁后真正执行且全部通过。
