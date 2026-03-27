package com.PMRGSolution.RENAISSANCE.Exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Default constructor (BAD_REQUEST fallback)
     */
    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    /**
     * Custom status constructor
     */
    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Constructor with cause for debugging
     */
    public BusinessException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}