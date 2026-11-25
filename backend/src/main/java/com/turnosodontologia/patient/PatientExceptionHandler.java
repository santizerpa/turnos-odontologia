package com.turnosodontologia.patient;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PatientExceptionHandler {

    private static final String INVALID_DATA_DETAIL = "Datos de paciente inválidos.";

    @ExceptionHandler({InvalidPatientDataException.class, MethodArgumentNotValidException.class,
        ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ProblemDetail handleInvalidRequest(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_DATA_DETAIL);
    }
}
