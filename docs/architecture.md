# ddd4j-javalin 架构

## 架构定位

`ddd4j-javalin` 是框架适配层，不复制 ddd4j 的领域模型、Repository 或 MQ 实现。它通过 Guice 将
ddd4j 的运行时端口装配到 Javalin，并补齐 Boot 自动配置在非 Spring 场景中的消费者体验。

```mermaid
flowchart TB
    App[业务应用与 samples] --> Bootstrap[Ddd4jJavalinApplication]
    Bootstrap --> Guice[Guice Injector]
    Guice --> Core[Ddd4jCoreGuiceModule]
    Guice --> Web[Web 生命周期适配]
    Guice --> Auth[认证 Provider 适配]
    Guice --> Data[数据与事务适配]
    Guice --> MQ[MQ Broker Module]
    Guice --> Ext[Extension Module]

    Core --> DCore[ddd4j core/runtime-guice]
    Web --> DWeb[ddd4j web-core/web-javalin]
    Auth --> DAuth[ddd4j auth-*]
    Data --> DData[ddd4j data-*]
    MQ --> DMQ[ddd4j mq-*]
    Ext --> DExt[ddd4j extensions]

    Tests[Testcontainers 合同与真实 IT] -.验证.-> Web
    Tests -.验证.-> Auth
    Tests -.验证.-> Data
    Tests -.验证.-> MQ
```

## 模块边界

| 边界 | Javalin 层职责 | ddd4j 权威实现 |
|---|---|---|
| Core | 安装完整 `Ddd4jGuiceModule`，管理 Injector 与关闭生命周期 | CommandBus、事件、投影和全局 SPI |
| Web | 请求上下文、鉴权策略、异常翻译、幂等、CORS、上传限制与超时 | `WebRequestLifecycle`、`Ddd4jJavalinWeb` |
| Auth | 将 SubjectProvider 装入 Guice；OIDC 提供通用 JWT/JWKS 资源服务器能力 | Sa-Token、Security、Shiro 原生 Provider |
| Data | Repository 注册、JPA EntityManagerFactory 与事务模板、DataScope/External/Data Logs 消费装配 | MyBatis/JPA/日志与数据端口 |
| MQ | 每个 Broker 的 Client/Properties 单例绑定 | 发布、消费、ACK/NACK、持久化与投递策略 |
| Extensions | 对有效上游扩展提供直接复用或轻量装配 | Excel、Monitor、PF4J、QLExpress、QRCode、Validation |

OIDC 与原生认证模块具有不同边界：只有 `ddd4j-javalin-auth-oidc` 负责 Keycloak JWT 验签与 HTTP
allow/deny；Sa-Token、Spring Security 和 Shiro 的 Keycloak 测试只证明容器兼容性，不宣称原生模块
直接消费 OIDC token。

## 可靠消息链路

```mermaid
sequenceDiagram
    participant Tx as 业务事务
    participant Outbox as PostgreSQL Outbox
    participant Relay as Outbox Relay
    participant Broker as MQ Broker
    participant Store as MQEventStorer
    participant Handler as 业务 Handler

    Tx->>Outbox: 同事务写入 PENDING
    Relay->>Broker: 持久化发布
    alt broker 确认成功
        Broker-->>Relay: confirm/成功响应
        Relay->>Outbox: 标记 PUBLISHED
    else 发布失败或不可路由
        Broker-->>Relay: error/basic.return
        Relay->>Outbox: 保留 PENDING + last_error
    end
    Broker->>Store: 投递后先持久化
    alt 持久化或处理失败
        Store-->>Broker: NACK/requeue
    else 持久化与处理成功
        Store->>Handler: 调用业务处理
        Handler-->>Broker: ACK
    end
```

RabbitMQ 已具备 persistent delivery、publisher confirm、`mandatory=true`、不可路由 return 失败、
持久化失败 NACK/requeue 与恢复后 ACK 的真实容器契约。其他 Broker 共享 ddd4j MQ 核心的 Inbox、
Outbox、重试与 DEAD 策略，但其协议级失败恢复证据必须按 Broker 独立核验，不能由普通 round-trip 代替。

## 测试基础设施边界

Testcontainers 固定为 `1.20.6`。MySQL、PostgreSQL、MariaDB、MongoDB、Kafka、RabbitMQ 与
LocalStack 优先使用 Testcontainers Java 的专用容器类型；无兼容专用模块的服务使用固定镜像标签的
`GenericContainer`。ONS 与 TDMQ 属于托管服务排除项，Mica MQTT 保留已记录的上游限制。Testcontainers
目录对“官方模块”和“社区模块”有明确区分，不能把任意 Docker 镜像称为官方模块。

## 启动序列

```mermaid
sequenceDiagram
    participant Main as main()
    participant App as Ddd4jJavalinApplication
    participant Guice as Guice Injector
    participant Web as Ddd4jJavalinWeb
    participant Javalin as Javalin 6/7

    Main->>App: run(args, basePkg, modules...)
    App->>App: 加载 Properties(CLI 覆盖)
    App->>Guice: createInjector(Ddd4jCoreGuiceModule + DddAnnotationModule + WebModule + extraModules)
    Note right of Guice: extraModules 通过 Modules.override 覆盖默认绑定
    Guice-->>App: Injector 就绪
    App->>Web: getInstance(Ddd4jJavalinWeb)
    App->>Javalin: Javalin.create(config -> web.configure(config))
    Note right of Javalin: web.configure 注册 before/after/exception 钩子
    Javalin->>Javalin: app.start(host, port)
    App-->>Main: 返回 Javalin 实例
    Javalin-->>Guice: STOPPING 时关闭 Ddd4jGuiceRuntime
```
