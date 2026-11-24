package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.Appointment;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages",
        indexes = @Index(name = "idx_outbox_appointment", columnList = "appointment_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_outbox_appointment_type", columnNames = {"appointment_id", "type"}))
public class OutboxMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = true, length = 20)
    private OutboxMessageType type;

    protected OutboxMessage() {}

    public OutboxMessage(Appointment appointment, String recipientEmail, String subject, String body, OutboxStatus status) {
        this.appointment = appointment;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public OutboxMessage(Appointment appointment, String recipientEmail, String subject, String body, OutboxStatus status, OutboxMessageType type) {
        this(appointment, recipientEmail, subject, body, status);
        this.type = type;
    }

    @PrePersist void auditCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public void markSimulatedSent(Instant sentAt) {
        if (status != OutboxStatus.PENDING) return;
        status = OutboxStatus.SIMULATED_SENT;
        this.sentAt = sentAt;
    }

    public UUID getId() { return id; }
    public Appointment getAppointment() { return appointment; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public OutboxStatus getStatus() { return status; }
    public OutboxMessageType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
