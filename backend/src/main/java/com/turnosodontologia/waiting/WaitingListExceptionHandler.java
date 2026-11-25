package com.turnosodontologia.waiting;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(basePackages = "com.turnosodontologia.waiting")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WaitingListExceptionHandler {

    private static final String INVALID_DATA_DETAIL = "Datos de inscripción inválidos.";

    @ExceptionHandler({InvalidWaitingListDataException.class, MethodArgumentNotValidException.class,
        ConstraintViolationException.class, HandlerMethodValidationException.class,
        HttpMessageNotReadableException.class})
    public ResponseEntity<java.util.Map<String, String>> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(java.util.Map.of("error", INVALID_DATA_DETAIL));
    }
}
