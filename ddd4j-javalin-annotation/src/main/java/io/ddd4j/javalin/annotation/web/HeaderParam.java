package io.ddd4j.javalin.annotation.web;

import java.lang.annotation.*;

/**
 * Javalin 请求头参数注解
 *
 * <p>Javalin 6 没有原生请求头参数注解，本注解由 ddd4j-javalin 反射注入框架实现。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface HeaderParam {
    String value();

    String defaultValue() default "";
}
