package io.ddd4j.javalin.annotation.web;

import java.lang.annotation.*;

/**
 * Javalin 查询参数注解
 * 
 * <p>Javalin 6 没有原生查询参数注解，本注解由 ddd4j-javalin 反射注入框架实现。
 * 
 * <p>业务代码使用：
 * <pre>
 * public List&lt;User&gt; listUsers(&#64;QueryParam("page") int page) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface QueryParam {
    String value();
    String defaultValue() default "";
}
