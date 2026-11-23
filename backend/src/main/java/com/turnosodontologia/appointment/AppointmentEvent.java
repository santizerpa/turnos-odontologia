package com.turnosodontologia.appointment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointment_events", indexes = @Index(name = "idx_event_appointment", columnList = "appointment_id"))
public class AppointmentEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 20)
    private AppointmentStatus fromStatus;

    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 20)
    private AppointmentStatus toStatus;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Actor actor;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppointmentEvent() {}

    public AppointmentEvent(Appointment appointment, AppointmentStatus fromStatus, AppointmentStatus toStatus, Actor actor, String reason) {
        this.appointment = appointment;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public AppointmentEvent(Appointment appointment, AppointmentStatus fromStatus, AppointmentStatus toStatus,
                            Actor actor, String reason, Instant createdAt) {
        this.appointment = appointment;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    @PrePersist void auditCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
