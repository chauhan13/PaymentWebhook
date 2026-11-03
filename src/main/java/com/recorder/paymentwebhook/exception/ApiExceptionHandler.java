package com.recorder.paymentwebhook.exception;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({EntityNotFoundException.class, PaymentNotFoundException.class})
    public ResponseEntity<Map<String, Object>> notFound(RuntimeException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> invalidRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "Request validation failed");
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
