package com.pmrgsolution.core.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password must be 8-30 characters and contain at least one uppercase letter, one lowercase letter, one digit, and one special character (e.g. @, #, $, %, etc.) with no spaces.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
