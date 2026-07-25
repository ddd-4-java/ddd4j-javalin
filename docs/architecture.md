# ddd4j-javalin 架构

## 三层架构

```
┌─────────────────────────────────────────────────────────────┐
│                  ddd4j-javalin-samples                      │
│      (rich-model / auth-* / mq-* / mybatis-testcontainers)  │
└─────────────────────┬───────────────────────────────────────┘
                      │ depends on
┌─────────────────────▼───────────────────────────────────────┐
│           ddd4j-javalin-* 适配层 (本仓库)                   │
│  ┌─────────┬─────────┬─────────┬──────────┬──────────────┐  │
│  │  web    │  data   │  auth   │   mq     │ extensions   │  │
│  │ Ddd4j   │ mybatis │ satoken │ 14broker │ qrcode       │  │
│  │ Javalin │ jpa     │ shiro   │ core+    │ qlexpress    │  │
│  │ App     │ logs    │ security│ kafka/   │ jackson      │  │
│  │ Web     │ data-   │ license │ rabbitmq │ monitor      │  │
│  │ Test    │ scope   │         │ redis/   │ pf4j/akka    │  │
│  │ Fixture │ external│         │ rocket/  │ excel        │  │
│  └─────────┴─────────┴─────────┴──────────┴──────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │      ddd4j-javalin-testcontainers (Testcontainers)   │  │
│  │  MySQL/Postgres/MariaDB/Mongo/Redis/Kafka/RabbitMQ  │  │
│  │  ActiveMQ/RocketMQ/Pulsar/NATS/MQTT/SQS/Keycloak    │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────┬───────────────────────────────────────┘
                      │ depends on
┌─────────────────────▼───────────────────────────────────────┐
│                       ddd4j 核心                            │
│  ┌─────────┬──────────┬─────────┬──────────┬────────────┐   │
│  │ ddd4j-  │ ddd4j-   │ ddd4j-  │ ddd4j-   │ ddd4j-     │   │
│  │ core    │ web-core │ data-*  │ auth-*   │ mq-*       │   │
│  │         │ web-     │ cache   │ license  │ extensions │   │
│  │         │ javalin  │ crypto  │          │            │   │
│  │         │ runtime- │ datascope│         │            │   │
│  │         │ guice    │ logs    │          │            │   │
│  │         │          │ external│          │            │   │
│  └─────────┴──────────┴─────────┴──────────┴────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 启动序列

```mermaid
sequenceDiagram
    participant Main as main()
    participant App as Ddd4jJavalinApplication
    participant Guice as Guice Injector
    participant Web as Ddd4jJavalinWeb
    participant Javalin as Javalin 7

    Main->>App: run(args, basePkg, modules...)
    App->>App: 加载 Properties(CLI 覆盖)
    App->>Guice: createInjector(Ddd4jGuiceModule + DddAnnotationModule + WebModule + ...)
    Guice-->>App: Injector 就绪
    App->>Web: getInstance(Ddd4jJavalinWeb)
    App->>Javalin: Javalin.create(config -> web.configure(config))
    Note right of Javalin: web.configure 注册 before/after/exception 钩子
    Javalin->>Javalin: app.start(host, port)
    App-->>Main: 返回 Javalin 实例
    App->>Main: 注册 shutdown hook(app::stop)
```