package com.turnosodontologia.appointment;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = PublicAppointmentTokenController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicAppointmentTokenExceptionHandler {
    @ExceptionHandler(PublicTokenInvalidException.class)
    public ResponseEntity<Map<String, Object>> invalid(PublicTokenInvalidException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Token inexistente o inválido"));
    }

    @ExceptionHandler(PublicTokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> expired(PublicTokenExpiredException exception) {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "Token vencido"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> illegalState(IllegalStateException exception) {
        if (exception.getMessage() != null
            && exception.getMessage().contains("24 horas")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Debe contactarse al consultorio para cancelar el turno"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "No se pudo confirmar el turno"));
    }
}
