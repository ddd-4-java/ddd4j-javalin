# ddd4j-javalin 示例工程

本目录只放 Javalin 运行时关注的示例：编程式路由、Guice 组装、Javalin Web 适配。通用领域模型优先复用 `io.ddd4j:ddd4j-sample-*`，避免在运行时仓库重复建模。

## 示例清单

| 示例                                  | 方向        | 说明 |
|-------------------------------------|-----------|------|
| `ddd4j-javalin-sample-rich-model`   | 普通充血模型   | 复用 `ddd4j-sample-rich-model`，通过 Guice 注入 `OrderApplicationService` 与内存 PO 仓储，Javalin 暴露订单 API |
| `ddd4j-javalin-sample-cqrs-person`  | CQRS / ES | 编程式路由 + Guice 命令服务 + 增量投影 |
| `ddd4j-javalin-sample-auth-satoken` | Auth      | Javalin + Sa-Token 示例 |
| `ddd4j-javalin-sample-auth-security` | Auth      | Javalin + Spring Security 兼容示例 |
| `ddd4j-javalin-sample-auth-shiro`   | Auth      | Javalin + Shiro 兼容示例 |

验证命令：

```bash
mvn -pl ddd4j-javalin-samples/ddd4j-javalin-sample-rich-model -am compile -DskipTests
```
