package io.ddd4j.javalin.api.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.ddd4j.javalin.api.annotation.Phone;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 数据校验注解实现类
 */
public class PhoneValueValidator implements ConstraintValidator<Phone, String> {

    private static PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    private Phone phoneValue;

    @Override
    public void initialize(Phone annotation) {
        this.phoneValue = annotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {

        Phonenumber.PhoneNumber referencePhonenumber = new Phonenumber.PhoneNumber();
        try {

            referencePhonenumber = phoneNumberUtil.parse(value, phoneValue.lang());
            boolean flag = phoneNumberUtil.isPossibleNumber(referencePhonenumber);
            if (!flag) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate(phoneValue.message())
                        .addConstraintViolation();
            }
            return flag;
        } catch (NumberParseException e) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(phoneValue.message())
                    .addConstraintViolation();
            return false;
        }
    }

}
