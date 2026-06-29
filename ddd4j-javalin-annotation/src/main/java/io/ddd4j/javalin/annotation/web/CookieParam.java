package io.ddd4j.javalin.annotation.web;

import java.lang.annotation.*;

/**
 * Javalin Cookie 参数注解（**Javalin 6 真正缺失的能力**）
 * 
 * <p>Javalin 6 没有原生 Cookie 路由参数注解，本注解由 ddd4j-javalin 反射注入框架实现。
 * 
 * <p>业务代码使用：
 * <pre>
 * public User getUser(&#64;CookieParam("session") String session) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CookieParam {
    String value();
    String defaultValue() default "";
}
