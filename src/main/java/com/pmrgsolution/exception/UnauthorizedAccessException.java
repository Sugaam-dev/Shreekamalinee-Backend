package com.pmrgsolution.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAccessException extends BusinessException {
    public UnauthorizedAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}