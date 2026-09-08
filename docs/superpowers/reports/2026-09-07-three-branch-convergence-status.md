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
- 三个实现提交已分别推送到 `origin` 和 `github`，推送后双远端 SHA 与本地逐分支一致。
- 六个 workflow 均已触发并到达终态：6.7.x [CI](https://github.com/ddd-4-java/ddd4j-javalin/actions/runs/34106299776) / [Integration](https://github.com/ddd-4-java/ddd4j-javalin/actions/runs/34106299513)，7.1.x [CI](https://github.com/ddd-4-java/ddd4j-javalin/actions/runs/34106303665) / [Integration](https://github.com/ddd-4-java/ddd4j-javalin/actions/runs/34106303715)，7.2.x [CI](https://github.com/ddd-4-java/ddd4j-javalin/actions/runs/34106308007) / [Integration](https://github.com/ddd-4-java/ddd4j-javalin/actions/runs/34106307960)。全部在执行任何 step 前被 GitHub 账户付款失败或 Actions spending limit 门禁拒绝，因而结论为 failure；这不是代码、Secret 或容器测试失败。
- ddd4j 3.0.x 最新 [Verify 运行 34096093007](https://github.com/ddd-4-java/ddd4j/actions/runs/34096093007) 已结束且结论为 failure，最近的 Deploy 同样失败。`feature/7.2.x` 通过的是已填充本地 Maven 仓库，不构成空白仓库远端消费证明。
- Maven 4 仍报告来自已部署 ddd4j 3.0.x BOM 的重复依赖管理冲突警告，需在上游模型中收敛。

## 结论

本地代码、版本矩阵、Maven 模型、单元测试和可运行容器 IT 已收敛，双远端同步已完成。远端验收仍由两项外部门禁阻塞：GitHub Actions 账户计费/额度，以及 ddd4j 3.0.x 成功部署后的空白 Maven 仓库消费验证。

## 2026-09-09 Phase A 本地检查点

CodeGraph 能力审计后，原规格已追加 Phase A-C。Phase A 已完成第一批运行时闭环：

- 6.7.x 补齐 Javalin 6 请求上下文、认证、Subject、Request/Trace ID、异常翻译、幂等和清理生命周期。
- 三线生产启动器默认安装完整 Core Guice Runtime，并在 Javalin stop 时撤销 SPI。
- 三线 Web 装配增加认证模式、可信代理和可关闭的默认幂等防护。
- 最终本地 clean test：6.7.x 95 tests、7.1.x 80 tests、7.2.x 74 tests，均为零 failure/error/skip。

该检查点尚未 push 或重新发布；自定义幂等 cache/TTL、未使用 server properties 以及 Phase B/C 仍未完成。
