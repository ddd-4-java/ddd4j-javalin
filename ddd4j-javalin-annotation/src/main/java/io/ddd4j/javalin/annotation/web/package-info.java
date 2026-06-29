/**
 * ddd4j-javalin Web 路由参数注解包
 *
 * <p>Javalin 6 完全没有注解，本包提供 7 个 ddd4j 自定义路由参数解析注解：
 * <ul>
 *   <li>{@link io.ddd4j.javalin.annotation.web.PathParam}     路径参数</li>
 *   <li>{@link io.ddd4j.javalin.annotation.web.QueryParam}    查询参数</li>
 *   <li>{@link io.ddd4j.javalin.annotation.web.FormParam}     表单参数</li>
 *   <li>{@link io.ddd4j.javalin.annotation.web.HeaderParam}   请求头参数</li>
 *   <li>{@link io.ddd4j.javalin.annotation.web.CookieParam}   Cookie 参数（**Javalin 6 真正缺失**）</li>
 *   <li>{@link io.ddd4j.javalin.annotation.web.BodyParam}     请求体参数</li>
 *   <li>{@link io.ddd4j.javalin.annotation.web.Context}       Context 注入</li>
 * </ul>
 */
package io.ddd4j.javalin.annotation.web;
