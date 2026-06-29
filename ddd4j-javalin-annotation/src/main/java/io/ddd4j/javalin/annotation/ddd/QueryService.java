package io.ddd4j.javalin.annotation.ddd;

import com.google.inject.Singleton;
import io.ddd4j.annotation.ddd.DDDAnnotation;

import java.lang.annotation.*;

/**
 * Javalin 查询服务
 */
@DDDAnnotation
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Singleton
@Inherited
public @interface QueryService {
}
