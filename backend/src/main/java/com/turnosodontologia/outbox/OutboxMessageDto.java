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
    Instant sentAt
) {
    public static OutboxMessageDto from(OutboxMessage message) {
        return new OutboxMessageDto(
            message.getId(),
            message.getAppointment() == null ? null : message.getAppointment().getId(),
            message.getRecipientEmail(),
            message.getSubject(),
            message.getBody(),
            message.getStatus(),
            message.getType(),
            message.getCreatedAt(),
            message.getSentAt()
        );
    }
}
