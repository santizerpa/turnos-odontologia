package com.turnosodontologia.appointment;

import com.turnosodontologia.patient.InvalidPatientDataException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = PublicBookingController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicBookingExceptionHandler {

    @ExceptionHandler(PublicBookingWindowException.class)
    public ResponseEntity<Map<String, Object>> window(PublicBookingWindowException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "La fecha solicitada está fuera de la ventana pública"));
    }

    @ExceptionHandler(PublicBookingDentistUnavailableException.class)
    public ResponseEntity<Map<String, Object>> dentistUnavailable(PublicBookingDentistUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Servicio temporalmente no disponible"));
    }

    @ExceptionHandler(PublicBookingSlotUnavailableException.class)
    public ResponseEntity<Map<String, Object>> slotUnavailable(PublicBookingSlotUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "La franja solicitada no está disponible"));
    }

    @ExceptionHandler(InvalidPatientDataException.class)
    public ResponseEntity<Map<String, Object>> invalidPatientData(InvalidPatientDataException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "Datos de reserva inválidos"));
    }

    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(AppointmentConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "La franja solicitada ya está ocupada"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> genericIntegrity(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "No se pudo procesar la reserva"));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> invalidRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "Datos de reserva inválidos"));
    }
}
