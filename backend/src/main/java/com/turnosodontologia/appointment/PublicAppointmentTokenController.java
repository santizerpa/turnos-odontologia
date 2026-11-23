package com.turnosodontologia.appointment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/appointments")
public class PublicAppointmentTokenController {
    private final AppointmentTokenService tokens;
    private final AppointmentService appointments;

    public PublicAppointmentTokenController(AppointmentTokenService tokens, AppointmentService appointments) {
        this.tokens = tokens;
        this.appointments = appointments;
    }

    @GetMapping("/{token}/confirm")
    public ResponseEntity<Map<String, Object>> confirmInfo(@PathVariable String token) {
        Appointment appointment = tokens.resolve(token).getAppointment();
        return informative(appointment);
    }

    @GetMapping("/{token}/cancel")
    public ResponseEntity<Map<String, Object>> cancelInfo(@PathVariable String token) {
        Appointment appointment = tokens.resolve(token).getAppointment();
        return informative(appointment);
    }

    private ResponseEntity<Map<String, Object>> informative(Appointment appointment) {
        return ResponseEntity.ok(Map.of(
            "startAt", appointment.getStartAt().toString(),
            "status", appointment.getStatus().name()));
    }

    @PostMapping("/{token}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable String token) {
        Appointment appointment = tokens.resolve(token).getAppointment();
        AppointmentStatus status = appointment.getStatus();

        if (status == AppointmentStatus.CONFIRMED) {
            return ResponseEntity.ok(Map.of("message", "ya confirmado", "status", "CONFIRMED"));
        }
        if (status == AppointmentStatus.CANCELLED
            || status == AppointmentStatus.ABSENT
            || status == AppointmentStatus.ATTENDED) {
            return ResponseEntity.status(409).body(Map.of("error", "El turno no puede confirmarse"));
        }

        appointments.changeStatus(
            appointment.getId(), AppointmentStatus.CONFIRMED, Actor.PATIENT,
            "Confirmación solicitada por el paciente mediante token");
        return ResponseEntity.ok(Map.of("message", "confirmación exitosa", "status", "CONFIRMED"));
    }

    @PostMapping("/{token}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable String token) {
        Appointment appointment = tokens.resolve(token).getAppointment();
        CancelAppointmentResult result = appointments.cancel(
            appointment.getId(), Actor.PATIENT,
            "Cancelación solicitada por el paciente mediante token");

        if (!result.cancelled()) {
            return ResponseEntity.ok(Map.of("message", "ya cancelado", "status", "CANCELLED"));
        }
        return ResponseEntity.ok(Map.of("message", "cancelación exitosa", "status", "CANCELLED"));
    }
}
