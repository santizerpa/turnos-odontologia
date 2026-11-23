package com.turnosodontologia.appointment;

import com.turnosodontologia.auth.principal.DentistPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointments;

    public AppointmentController(AppointmentService appointments) {
        this.appointments = appointments;
    }

    @GetMapping
    public List<AppointmentResponse> agenda(@AuthenticationPrincipal DentistPrincipal principal,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return appointments.agenda(principal.id(), from, to).stream().map(AppointmentResponse::from).toList();
    }

    @PatchMapping("/{id}/status")
    public Map<String, Object> changeStatus(@PathVariable UUID id,
                                            @RequestBody AppointmentStatusRequest request) {
        if (request.status() == AppointmentStatus.CANCELLED) {
            CancelAppointmentResult result = appointments.cancel(id, Actor.DENTIST, request.reason());
            return Map.of("cancelled", result.cancelled(), "message", result.message());
        }
        Appointment appointment = appointments.changeStatus(id, request.status(), Actor.DENTIST, request.reason());
        return Map.of("status", appointment.getStatus().name(), "message", "estado actualizado");
    }
}
