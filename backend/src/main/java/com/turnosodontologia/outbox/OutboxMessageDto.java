package com.turnosodontologia.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessageDto(
    UUID id,
    UUID appointmentId,
    String recipientEmail,
    String subject,
    String body,
    OutboxStatus status,
    OutboxMessageType type,
    Instant createdAt,
    Instant sentAt,
    String patientName,
    String patientDni
) {
    public static OutboxMessageDto from(OutboxMessage message) {
        var appointment = message.getAppointment();
        var patient = appointment == null ? null : appointment.getPatient();
        return new OutboxMessageDto(
            message.getId(),
            appointment == null ? null : appointment.getId(),
            message.getRecipientEmail(),
            message.getSubject(),
            message.getBody(),
            message.getStatus(),
            message.getType(),
            message.getCreatedAt(),
            message.getSentAt(),
            patient == null ? null : patient.getFirstName() + " " + patient.getLastName(),
            patient == null ? null : patient.getDni()
        );
    }
}
