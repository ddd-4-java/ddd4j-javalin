# ddd4j-javalin 三分支收敛验证报告

日期：2026-09-07

规格事实源：`docs/superpowers/specs/2026-09-07-three-branch-convergence-design.md`

## 本地实现基线

| 分支 | 实现提交 | 构建契约 | ddd4j / Javalin | 干净单元回归 |
|---|---|---|---|---|
| `feature/6.7.x` | `d97bfe4` | Maven 3 / POM 4.0.0 / JDK 17 | `1.0.x.20260630-SNAPSHOT` / `6.7.0` | 79 tests，0 failure，0 error，0 skipped |
| `feature/7.1.x` | `467c040` | Maven 3 / POM 4.0.0 / JDK 17 | `2.0.x.20260630-SNAPSHOT` / `7.1.0` | 72 tests，0 failure，0 error，0 skipped |
| `feature/7.2.x` | `ad774db` | Maven 4 / POM 4.1.0 / JDK 21 | `3.0.x.20260630-SNAPSHOT` / `7.2.3` | 66 tests，0 failure，0 error，0 skipped |

以上单元计数来自各分支独立执行的 `clean test` 后汇总 Surefire XML，不含容器 IT。

## 容器级证据

三条分支均已逐项执行并通过：

- MySQL CRUD：2 tests / branch。
- Keycloak：Sa-Token、Spring Security、Shiro 各 1 test / branch；使用 `testcontainers-keycloak 3.7.0` 与 `quay.io/keycloak/keycloak:26.2`。
- Broker：SQS、NATS、Kafka、RocketMQ、RabbitMQ、ActiveMQ Artemis、Pulsar、Redis Stream、MQTT，各 2 tests / branch，全部为真实 publish → broker → consume round-trip。

ONS、TDMQ 是需要外部凭据的托管服务，不纳入本地容器验收；Mica MQTT 因上游 AIO 问题保持单测试显式禁用。PostgreSQL outbox sample 在部分上游线没有可解析的发布构件，未作为三分支共同门禁。

## CI 与发布证据边界

- 两个 workflow 已按分支修正 JDK、Maven Wrapper、监听分支和 `MAVEN_SETTINGS_XML` 原始 XML 校验。
- 当前变更仅存在于本地提交，尚未获得 push 授权，因此没有最终提交对应的 GitHub Actions URL 或 conclusion。
- 只读远端核对显示 `origin` 与 `github` 三分支 SHA 完全一致：6.7.x 为 `80d300d`、7.1.x 为 `ac5d795`、7.2.x 为 `e2db96e`；三个本地分支均领先对应远端。
- ddd4j 3.0.x 的远端 Verify/Deploy 当前仍为红色；`feature/7.2.x` 通过的是已填充本地 Maven 仓库，不构成空白仓库远端消费证明。
- Maven 4 仍报告来自已部署 ddd4j 3.0.x BOM 的重复依赖管理冲突警告，需在上游模型中收敛。

## 结论

本地代码、版本矩阵、Maven 模型、单元测试和可运行容器 IT 已收敛。远端完成状态仍由两项门禁阻塞：三分支 push 后的 GitHub Actions，以及 ddd4j 3.0.x 成功部署后的空白 Maven 仓库消费验证。
