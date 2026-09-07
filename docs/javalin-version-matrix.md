# ddd4j-javalin 三分支兼容矩阵

本矩阵是 2026-09-07 三分支收敛后的构建契约。分支版本、上游 ddd4j、Javalin、Maven 模型和 JDK 必须整组变更，不能单独漂移。

| ddd4j-javalin 分支 | 本项目版本 | ddd4j 分支 / 版本 | Javalin | Maven Wrapper | POM 模型 / 聚合元素 | 构建 JDK |
|---|---|---|---|---|---|---|
| `feature/6.7.x` | `6.7.x.20260630-SNAPSHOT` | `feature/1.0.x` / `1.0.x.20260630-SNAPSHOT` | `6.7.0` | Maven `3.9.16` | `4.0.0` / `<modules>` | 17 |
| `feature/7.1.x` | `7.1.x.20260630-SNAPSHOT` | `feature/2.0.x` / `2.0.x.20260630-SNAPSHOT` | `7.1.0` | Maven `3.9.16` | `4.0.0` / `<modules>` | 17 |
| `feature/7.2.x` | `7.2.x.20260630-SNAPSHOT` | `feature/3.0.x` / `3.0.x.20260630-SNAPSHOT` | `7.2.3` | Maven `4.0.0-rc-6` | `4.1.0` / `<subprojects>` | 21 |

## Maven 兼容边界

- `feature/6.7.x` 和 `feature/7.1.x` 消费 Maven 3 模型的上游，全部生产 POM 使用 `modelVersion 4.0.0` 和 `<modules>`。
- `feature/7.2.x` 消费 ddd4j 3.0.x，全部生产 POM 使用 Maven 4 的 `modelVersion 4.1.0` 和 `<subprojects>`。
- Maven 4 的内部父项目依靠相对路径推断；不在 `<parent>` 中同时声明本地相对坐标和重复 GAV。
- `BuildLineContractTest` 会同时验证版本、POM 模型、聚合元素、Wrapper、JDK 和 GitHub Actions 分支，防止矩阵再次漂移。

## CI 约束

- 两个 GitHub Actions workflow 都只监听当前分支，并使用该分支规定的 JDK 和 `./mvnw`。
- 组织 Secret `MAVEN_SETTINGS_XML` 必须以原始 XML 内容注入并在执行 Maven 前校验；它不是文件路径。
- 禁止 job 级 `continue-on-error: true` 掩盖构建或集成测试失败。

## 依赖维护规则

1. Javalin、Jetty 和最低 Java 运行版本视为同一升级单元。
2. Jetty 版本由 Javalin 传递依赖决定；适配模块不得各自固定冲突版本。
3. 修改 ddd4j 主线映射前，先验证上游构件已经发布，再使用空白 Maven 本地仓库做消费证明。
4. 跨 Javalin 或 ddd4j 主线的变更必须执行单元测试、MySQL、Keycloak 和 broker 容器回归。

## 参考

- [Maven 4 新特性](https://maven.apache.org/whatsnewinmaven4.html)
- [Maven 4.1.0 模型参考](https://maven.apache.org/ref/4-LATEST/api/maven-api-model/maven.html)
- [Javalin 官方文档](https://javalin.io/documentation)
- [Javalin 6 到 7 迁移指南](https://javalin.io/migration-guide-javalin-6-to-7)
