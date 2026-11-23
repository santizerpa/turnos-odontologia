package com.turnosodontologia.appointment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/appointments")
public class PublicAppointmentTokenController {
    private final AppointmentTokenService tokens;

    public PublicAppointmentTokenController(AppointmentTokenService tokens) {
        this.tokens = tokens;
    }

    @PostMapping("/{token}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable String token) {
        AppointmentToken resolved = tokens.resolve(token);
        return ResponseEntity.ok(Map.of("status", resolved.getAppointment().getStatus().name()));
    }
}
