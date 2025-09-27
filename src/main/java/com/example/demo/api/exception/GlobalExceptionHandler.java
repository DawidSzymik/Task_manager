// src/main/java/com/example/demo/api/exception/GlobalExceptionHandler.java
package com.example.demo.api.exception;

import com.example.demo.api.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.demo.api")
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                "ENTITY_NOT_FOUND",
                e.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_FAILED",
                "Validation failed for one or more fields",
                fieldErrors
        );
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_ARGUMENT",
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                "ACCESS_DENIED",
                "Access denied: " + e.getMessage(),
                HttpStatus.FORBIDDEN.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                "RUNTIME_ERROR",
                e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));

        System.err.println("Unexpected error: " + e.getMessage());
        e.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}