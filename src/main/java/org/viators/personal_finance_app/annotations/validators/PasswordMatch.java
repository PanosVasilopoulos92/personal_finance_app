package org.viators.personal_finance_app.annotations.validators;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.viators.personal_finance_app.dtos.UserDTOs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE}) // At class level because it checks two (more than one) fields of same class/record
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatch.Validator.class)
public @interface PasswordMatch {
    String message() default "Passwords do not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PasswordMatch, UserDTOs.CreateUserRequest> {
        @Override
        public boolean isValid(UserDTOs.CreateUserRequest createUserRequest, ConstraintValidatorContext constraintValidatorContext) {
            if (createUserRequest == null || createUserRequest.password() == null || createUserRequest.confirmPassword() == null) {
                return true;
            }

            return createUserRequest.password().equals(createUserRequest.confirmPassword());
        }
    }
}

