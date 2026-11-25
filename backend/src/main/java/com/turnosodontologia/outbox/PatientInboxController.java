package com.turnosodontologia.outbox;

import com.turnosodontologia.patient.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Demo-only simulation of the patient's e-mail inbox. Disabled unless {@code app.demo.inbox.enabled=true}:
 * whoever knows an e-mail can read its messages and their confirm/cancel links, which is accepted only for the demo.
 */
@RestController
@RequestMapping("/api/public/inbox")
public class PatientInboxController {
    private final OutboxMessageRepository outbox;
    private final boolean enabled;

    public PatientInboxController(OutboxMessageRepository outbox,
                                  @Value("${app.demo.inbox.enabled:false}") boolean enabled) {
        this.outbox = outbox;
        this.enabled = enabled;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> inbox(@RequestParam(required = false) String email) {
        if (!enabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Bandeja simulada deshabilitada"));
        }
        String normalized;
        try {
            normalized = Patient.normalizeEmail(email);
        } catch (IllegalArgumentException invalid) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", "Email inválido"));
        }
        List<PatientInboxMessageDto> messages = outbox.findByRecipientEmailOrderByCreatedAtDesc(normalized).stream()
            .map(PatientInboxMessageDto::from)
            .toList();
        return ResponseEntity.ok(messages);
    }
}
