package com.turnosodontologia.appointment;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.turnosodontologia.appointment")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AppointmentExceptionHandler {
    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(AppointmentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DentistConfirmationNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> dentistConfirmation(DentistConfirmationNotAllowedException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> invalid(Exception exception) {
        String message = "Solicitud inválida";
        if (exception instanceof InvalidAppointmentRangeException) message = "Rango de agenda inválido";
        else if (exception instanceof InvalidAppointmentTransitionException) message = "Transición de estado inválida";
        else if (exception instanceof AppointmentDentistNotFoundException) message = "Odontólogo inexistente";
        else if (exception instanceof AppointmentPatientNotFoundException) message = "Paciente inexistente";
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", message));
    }

    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(AppointmentConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "La franja solicitada ya está ocupada"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrityViolation(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "No se pudo completar la operación"));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> badRange(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "Parámetros from y to inválidos o faltantes"));
    }
}
