# Javalin 版本 × JDK 基线矩阵（选型依据）

> 2026-08-16 调研沉淀。验证方法：解包每条主线终点版本 jar 读 `Javalin.class` 字节码 major、核对 `javalin-parent` POM `<jdk.version>` 与捆绑 Jetty 版本、官方文档交叉验证。
> 结论：**网传"Javalin 6 要求 Java 17"不准确**——JDK 门槛共三次跳变。

## 版本矩阵

| 版本线（终点版本） | JDK 要求 | 捆绑 Jetty | 验证依据 |
|---|---|---|---|
| 0.x（→ 0.5.4）、1.x（→ 1.7.0）、2.x（→ 2.8.0）、3.x（→ 3.13.13） | Java 8 | Jetty 9 | 字节码 major 52 |
| 4.x（→ 4.6.8） | Java 8 | Jetty 9.4.x | 字节码 major 52 |
| 5.x（→ 5.6.5） | Java 11 | Jetty 11 | 字节码 major 55；POM `jdk.version=11` |
| 6.x（→ 6.7.0） | Java 11 | Jetty 11 | 字节码 major 55；POM `jdk.version=11`；v6 归档文档 |
| 7.x（当前 7.2.3） | Java 17 | Jetty 12.1.x | 字节码 major 61；POM `jdk.version=17`；官方文档 "requires Java 17+, and Jetty 12+" |

（major 52=Java 8，55=Java 11，61=Java 17；Maven Central 共 166 个版本）

## 本项目选型与实测（2026-08-16）

| 项 | 本项目 | 与官方基线一致性 |
|---|---|---|
| Javalin | **6.7.0**（已迁移，feature/6.7.x） | ✅ Java 11+ 基线（全仓 `<java.version>17</java.version>`，兼容） |
| Jetty（web 模块透传） | 11.x（由 `io.javalin:javalin:6.7.0` 传递） | ✅ 由 Javalin 6.7.0 传递，版本统一 |
| Jetty（qrcode 模块） | 统一透传 Javalin 6.7.0 传递版本 | ✅ 无显式 pin |
| ddd4j web 适配 | `ddd4j-web-javalin6`（applyTo 契约） | ✅ 核心侧双轨模块，公开签名超集 |
| 迁移状态 | **已迁移**（Javalin 6.7.0 + ddd4j-web-javalin6 + applyTo 契约） | 6.7.x 行已解阻塞 |

## 维护规则（强约束）

1. **Jetty 版本由 Javalin 传递决定，禁止在业务/扩展模块显式 pin Jetty**——历史上核心 ddd4j-dependencies 曾把 `jetty-server`/`jetty-util` pin 到 9.4（Java 8 时代），导致 qrcode 模块被迫手工覆盖；该 pin 已不在当前链路，模块级 pin 也已全部移除。若未来核心 BOM 再现 9.4/11.x pin，应在 `ddd4j-javalin-dependencies` 的 `<dependencyManagement>` 统一覆盖为 Javalin 传递版本，而非模块各自为政。
2. **升级 Javalin 主线版本 = 同步核对 JDK 与 Jetty 双基线**（三者绑定跳变：5.0/Jetty11/Java11，7.0/Jetty12/Java17）。
3. **跨主线升级需回归容器级 IT**（`mvn verify -Pjavalin-integration-tests`）。

## 已知坑

- **Javalin 5.6.4**：用 JDK 21 构建，在 Java 17 上会崩（[issue #2239](https://github.com/javalin/javalin/issues/2239)）；停留 5.x 线请用 5.6.5。
- **虚拟线程**：6.x/7.x 的 `config.useVirtualThreads = true` 需 JDK 21+，非框架最低要求。

## 来源

- [Javalin 官方文档](https://javalin.io/documentation)
- [Javalin 6.0.0 稳定版公告](https://javalin.io/news/javalin-6.0.0-stable.html)
- [Javalin v6 归档文档](https://javalin.io/archive/docs/v6.X.html)
- [6→7 迁移指南](https://javalin.io/migration-guide-javalin-6-to-7) / [5→6 迁移指南](https://javalin.io/migration-guide-javalin-5-to-6)