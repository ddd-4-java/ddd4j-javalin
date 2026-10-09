package io.ddd4j.javalin.api.annotation;

import io.ddd4j.javalin.api.validation.NumberValueValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Constraint(validatedBy = {NumberValueValidator.class})
public @interface NumberValue {

    String regex() default "^[0-9\\-]+$";

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
