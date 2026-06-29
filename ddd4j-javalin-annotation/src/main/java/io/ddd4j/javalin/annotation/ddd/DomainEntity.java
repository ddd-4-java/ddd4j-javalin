package io.ddd4j.javalin.annotation.ddd;

import com.google.inject.Singleton;
import io.ddd4j.annotation.ddd.DDDAnnotation;
import java.lang.annotation.*;

/**
 * Javalin 领域实体
 */
@DDDAnnotation
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Singleton
@Inherited
public @interface DomainEntity {

    /** 是否是聚合根 */
    boolean aggregateRoot() default false;
}
