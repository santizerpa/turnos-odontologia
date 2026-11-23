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

    @ExceptionHandler(CancellationWindowClosedException.class)
    public ResponseEntity<Map<String, Object>> cancellationWindowClosed(CancellationWindowClosedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "Debe contactarse al consultorio para cancelar el turno"));
    }

    @ExceptionHandler(PublicAppointmentCancelIllegalStateException.class)
    public ResponseEntity<Map<String, Object>> cancelIllegalState(PublicAppointmentCancelIllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "No se pudo cancelar el turno"));
    }

    @ExceptionHandler(PublicAppointmentConfirmIllegalStateException.class)
    public ResponseEntity<Map<String, Object>> confirmIllegalState(PublicAppointmentConfirmIllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "No se pudo confirmar el turno"));
    }
}
