package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.patient.Patient;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages", indexes = @Index(name = "idx_outbox_appointment", columnList = "appointment_id"))
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

    protected OutboxMessage() {}

    public OutboxMessage(Appointment appointment, String recipientEmail, String subject, String body, OutboxStatus status) {
        this.appointment = appointment;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.createdAt = Instant.now();
    }

    @PrePersist void auditCreate() { if (createdAt == null) createdAt = Instant.now(); }
    public Appointment getAppointment() { return appointment; }
}
