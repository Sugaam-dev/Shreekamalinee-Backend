package com.PMRGSolution.RENAISSANCE.Exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle DTO Validation (e.g., @NotBlank, @Email, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", "Check validationErrors field", request, errors);
    }

    // 2. Handle Missing Request Parameters
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("The required parameter '%s' is missing", ex.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST, "Missing Parameter", message, request, null);
    }

    // 3. Handle Malformed JSON or Missing Request Body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed Request", "Required request body is missing or invalid", request, null);
    }

    // 4. Handle Login/Security Failures
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuthFailures(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password", request, null);
    }

    // 5. Handle Permission/Role Issues
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource", request, null);
    }

    // 6. Handle Database Conflicts (e.g., duplicate email registration)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflicts(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Conflict", "The email or resource already exists", request, null);
    }

    // 7. Handle Business Logic Errors (Custom BusinessException)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogic(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error: {}", ex.getMessage());
        return buildResponse(ex.getStatus(), "Business Error", ex.getMessage(), request, null);
    }

    // 8. Handle 404 Not Found (Document, Category, User missing)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request, null);
    }

    // 9. Handle File Size Limit (Crucial for PDF Uploads)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large", "The uploaded PDF exceeds the maximum allowed size", request, null);
    }

    // 10. Generic Fallback (Internal Server Errors)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobal(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: ", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", "An unexpected error occurred", request, null);
    }
    
 // 11. Handle Wrong HTTP Method (e.g., POST instead of GET)
    // Professional Status: 405 Method Not Allowed
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, 
            HttpServletRequest request) {
        
        String message = String.format("Method '%s' is not supported for this path. Please use %s", 
                ex.getMethod(), ex.getSupportedHttpMethods());

        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", message, request, null);
    }

    // 12. Handle Media Type Not Supported (e.g., sending XML instead of JSON)
    // Professional Status: 415 Unsupported Media Type
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException ex, 
            HttpServletRequest request) {
        
        String message = String.format("Media type '%s' is not supported. Supported types are: %s", 
                ex.getContentType(), ex.getSupportedMediaTypes());

        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", message, request, null);
    }

    // 13. Handle Data Type Mismatch (e.g., passing "abc" for a UUID/Long path variable)
    // Professional Status: 400 Bad Request
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, 
            HttpServletRequest request) {
        
        String message = String.format("The parameter '%s' should be of type %s", 
                ex.getName(), ex.getRequiredType().getSimpleName());

        return buildResponse(HttpStatus.BAD_REQUEST, "Type Mismatch", message, request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message, HttpServletRequest request, Map<String, String> vErrors) {
        return new ResponseEntity<>(ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .validationErrors(vErrors)
                .build(), status);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Input", ex.getMessage(), request, null);
    }
 // Add to GlobalExceptionHandler.java
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Operation", ex.getMessage(), request, null);
    }
 
 // 14. Handle Logout/Auth missing exceptions without a full stack trace
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuthNotFound(
            AuthenticationCredentialsNotFoundException ex, 
            HttpServletRequest request) {
        
        // Log as a single line WARN, not an ERROR with a stack trace
        log.warn("Security Alert at {}: {}", request.getRequestURI(), ex.getMessage()); 
        
        return buildResponse(
            HttpStatus.UNAUTHORIZED, 
            "Unauthorized", 
            ex.getMessage(), 
            request, 
            null
        );
    }
    
}