package com.turnosodontologia.appointment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = PublicAppointmentTokenController.class)
public class PublicAppointmentTokenExceptionHandler {
    @ExceptionHandler(PublicTokenInvalidException.class)
    public ResponseEntity<Map<String, Object>> invalid(PublicTokenInvalidException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Token inexistente o inválido"));
    }

    @ExceptionHandler(PublicTokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> expired(PublicTokenExpiredException exception) {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "Token vencido"));
    }
}
