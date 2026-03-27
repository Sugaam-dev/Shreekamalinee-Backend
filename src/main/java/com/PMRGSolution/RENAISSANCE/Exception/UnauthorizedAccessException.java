package com.PMRGSolution.RENAISSANCE.Exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAccessException extends BusinessException {
    public UnauthorizedAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}