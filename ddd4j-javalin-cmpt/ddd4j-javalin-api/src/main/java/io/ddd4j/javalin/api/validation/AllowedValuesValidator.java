package io.ddd4j.javalin.api.validation;

import io.ddd4j.javalin.api.annotation.AllowableValues;
import org.springframework.util.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class AllowedValuesValidator implements ConstraintValidator<AllowableValues, String> {

    List<String> allows;
    boolean nullable;

    @Override
    public void initialize(AllowableValues annotation) {
        nullable = annotation.nullable();
        allows = Arrays.asList(annotation.allows().split(","));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (nullable && !StringUtils.hasText(value)) {
            return true;
        }
        return allows.contains(value);
    }
}
