package com.turnosodontologia.outbox;

import java.time.Instant;
import java.util.UUID;

/** Simulated patient inbox entry: only what the patient's own mailbox would show. */
public record PatientInboxMessageDto(
    UUID id,
    String subject,
    String recipientEmail,
    String patientDni,
    Instant appointmentStartAt,
    Integer appointmentDurationMinutes,
    Instant createdAt,
    String body
) {
    static PatientInboxMessageDto from(OutboxMessage message) {
        var appointment = message.getAppointment();
        return new PatientInboxMessageDto(
            message.getId(),
            message.getSubject(),
            message.getRecipientEmail(),
            appointment == null ? null : appointment.getPatient().getDni(),
            appointment == null ? null : appointment.getStartAt(),
            appointment == null ? null : appointment.getDurationMinutes(),
            message.getCreatedAt(),
            message.getBody()
        );
    }
}
