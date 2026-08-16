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

## 分支策略：两线制（2026-08-16 定案）

**不按 Javalin 每个 minor 开分支线**（6.3/6.4/6.5/6.6/7.0/7.1 六条不开）——矩阵内 8 条线实际只有 2 个技术基线（6.x=Java11 字节码+Jetty 11，7.x=Java17+Jetty 12），组内适配层代码 99% 相同，多线维护是纯成本。用末代版本覆盖存量用户：

| 活跃分支线 | ddd4j-javalin 版本 | Javalin 库 | ddd4j 核心 | 定位 |
|---|---|---|---|---|
| `feature/7.2.x` | `7.2.x-SNAPSHOT` | 7.2.3 | feature/3.0.x | **主战线**：承接全部最新成果（unsafe.routes API 形态） |
| `feature/6.7.x` | `6.7.x-SNAPSHOT` | 6.7.0 | feature/2.0.x | ⚠️ 分支已就位（由 feature/6.3.x 改名，双 remote 已同步）；降级受核心阻塞（见下），待 B/C 路径决策。分支内容暂为 7 形态成果 |

**JDK 17 统一最低基线**（两线同规）：
- 运行/CI/工具链统一 JDK 17；不做 JDK 8 兼容（Javalin 5.0 起已出局）。
- `feature/6.7.x` 编译目标 `--release 11`（产物兼容面更广），但最低运行 JDK 仍为 17。
- `feature/7.2.x` 编译与运行均为 17。

## 6.7.x 线可行性探测（2026-08-16，架构智能体报告）

**核心硬阻塞**：核心仓库任何分支/tag 均无 Javalin 5/6 基线的 `ddd4j-web-javalin`——m2 中 v1(1.0.x)=Javalin 4.6.8（`io.javalin.core` 包，6.7.0 无法加载）、v2/v3(2.0.x/3.0.x)=Javalin 7.2.2（`config.routes.*` API，Javalin 6 不存在）。`ddd4j-javalin-web` 依赖核心 `Ddd4jJavalinWeb`，直接降库版本会 NoSuchMethodError。

**API 差异面**（本仓库）：`app.unsafe.routes.*` 共 51 处/14 文件 → Javalin 6 等价 `app.get/post/exception(...)` 实例方法（纯机械替换）；核心侧 `config.routes.*` → Javalin 6 需移到实例方法注册；Context 差异 `method()/status()/req` 三处。

**三路径**：
| 路径 | 可行性 | 工程量 | 依赖 |
|---|---|---|---|
| A. 等核心开 javalin6 基线分支 | 低（历史无存量） | 核心侧 2-3 天 | 核心团队承诺，不可控 |
| B. ddd4j-javalin-web 自研 Javalin 6 装配（Ddd4jJavalinWeb6，v1 源码为模板） | 中（51 处机械替换 + 1 个装配类 + 6 文件适配） | 本仓库 3-5 天 | 无，但分叉核心 Web SPI 契约需手工同步 |
| C. 放弃 6.7.x 线，7.2.x 统一覆盖 | 最高（零成本） | 0 | 代价：Jetty 11/Java 11 存量用户无覆盖 |

**决策门**：是否存在真实的 Javalin 6 / Java 11 / Jetty 11 下游需求——无则选 C，有则选 B。

## 本项目选型与实测（2026-08-16，feature/7.2.x）

| 项 | 本项目 | 与官方基线一致性 |
|---|---|---|
| Javalin | 7.2.3（自 7.2.2 升级，132 测试回归通过） | ✅ Java 17 基线（全仓 `<java.version>17</java.version>`） |
| Jetty（透传） | 12.1.x（由 javalin 传递，统一） | ✅ |
| Jetty（qrcode 模块） | ~~显式 pin 12.1.5~~ → 已删除，统一透传 | ✅ 修复了同模块混版本 |

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