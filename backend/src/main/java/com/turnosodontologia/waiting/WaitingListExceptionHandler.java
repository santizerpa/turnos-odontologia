package com.turnosodontologia.waiting;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(basePackages = "com.turnosodontologia.waiting")
public class WaitingListExceptionHandler {

    private static final String INVALID_DATA_DETAIL = "Datos de inscripción inválidos.";

    @ExceptionHandler({InvalidWaitingListDataException.class, MethodArgumentNotValidException.class,
        ConstraintViolationException.class, HandlerMethodValidationException.class,
        HttpMessageNotReadableException.class})
    public ProblemDetail handleInvalidRequest(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_DATA_DETAIL);
    }
}
