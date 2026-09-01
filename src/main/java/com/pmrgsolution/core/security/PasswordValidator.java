package com.pmrgsolution.core.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // 8-30 characters, >=1 uppercase, >=1 lowercase, >=1 digit, >=1 special character, no whitespace
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-*()/?><,.:;\"'{}|~`\\[\\]\\\\])(?=\\S+$).{8,30}$";
    private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        return PATTERN.matcher(password).matches();
    }
}
