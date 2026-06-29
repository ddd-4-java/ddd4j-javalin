package io.ddd4j.javalin.annotation.web;

import java.lang.annotation.*;

/**
 * Javalin Context 注入注解
 * 
 * <p>Javalin 必须手动传递 Context，本注解由 ddd4j-javalin 反射注入框架实现。
 * 
 * <p>业务代码使用：
 * <pre>
 * public User getUser(&#64;Context io.javalin.http.Context ctx) {
 *     String id = ctx.pathParam("id");
 *     // ...
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Context {
}
