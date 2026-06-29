package io.ddd4j.javalin.annotation.web;

import java.lang.annotation.*;

/**
 * Javalin 路径参数注解
 *
 * <p>Javalin 6 没有原生路径参数注解，本注解由 ddd4j-javalin 反射注入框架实现。
 *
 * <p>业务代码使用：
 * <pre>
 * public User getUser(&#64;PathParam("id") String id) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface PathParam {
    String value();

    String defaultValue() default "";
}
