# Javalin 请求生命周期

ddd4j-javalin 通过 `Ddd4jJavalinWeb.configure(JavalinConfig)` 安装 3 个钩子:

## 1. before 钩子(openContext)

```
incoming request
  → extract headers
  → WebOtelSupport.startServerSpan(method, path, headers)
  → WebOtelSupport.activate(span)
  → WebRequestContext context = createContext(request)
  → RequestState state = new RequestState(WebContextScope.open(context))
  → WebRequestLifecycle.authenticate(context)
        .ifPresent(auth -> ThreadContext.bind(auth.subject()))
  → WebIdempotencyLifecycle.open(context, idempotencyKey)
        .ifPresent(state::idempotencyScope)
```

## 2. exception 钩子(handleException)

```
exception thrown anywhere downstream
  → WebError error = WebExceptionTranslator.translate(exception)
  → if 5xx: log.error
  → WebOtelSupport.recordError(span, exception)
  → context.status(error.status()).json(error.toResponse())
  → closeContext(context, false)
```

## 3. after 钩子(completeContext)

```
response returning to client
  → WebOtelSupport.endServerSpan(span, status)
  → closeContext(context, status < 400)
```

## 关键 SPI

| SPI | 作用 | 默认实现 |
|---|---|---|
| `WebRequestContextFactory` | 从 `Context` 构造 `WebRequestContext`(tenant/ip/locale) | `new WebRequestContextFactory()` |
| `WebRequestLifecycle` | 认证 + 访问策略匹配 | `WebRequestLifecycle(auth, policy)` |
| `WebExceptionTranslator` | 业务异常 → `WebError`(HTTP status + JSON) | `DefaultWebExceptionTranslator` |
| `WebIdempotencyLifecycle` | 幂等键去重 | 可选,需业务方提供 |

## 自定义

```java
Ddd4jJavalinAutoConfiguration config = new Ddd4jJavalinAutoConfiguration(properties) {
    @Provides @Singleton
    WebExceptionTranslator translator() {
        return new MyBusinessExceptionTranslator();
    }
};
```