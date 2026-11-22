package com.turnosodontologia.appointment;

import com.turnosodontologia.common.AuditableEntity;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.patient.Patient;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointment_dentist", columnList = "dentist_id"),
    @Index(name = "idx_appointment_patient", columnList = "patient_id")
})
public class Appointment extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "dentist_id", nullable = false)
    private Dentist dentist;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by", nullable = false, length = 20)
    private CreatedBy createdBy;

    @Column(name = "active_slot", unique = true)
    private Instant activeSlot;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private long version;

    protected Appointment() {}

    public static Appointment create(Dentist dentist, Patient patient, Instant startAt, int durationMinutes, CreatedBy createdBy) {
        if (dentist == null || patient == null || startAt == null) throw new IllegalArgumentException("Datos de turno requeridos");
        if (durationMinutes != 30) throw new IllegalArgumentException("Duración fija de 30 minutos");
        Appointment appointment = new Appointment();
        appointment.dentist = dentist;
        appointment.patient = patient;
        appointment.startAt = startAt;
        appointment.durationMinutes = durationMinutes;
        appointment.status = AppointmentStatus.PENDING;
        appointment.createdBy = createdBy;
        appointment.activeSlot = startAt;
        return appointment;
    }

    public UUID getId() { return id; }
    public Dentist getDentist() { return dentist; }
    public Patient getPatient() { return patient; }
    public Instant getStartAt() { return startAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public AppointmentStatus getStatus() { return status; }
    public CreatedBy getCreatedBy() { return createdBy; }
    public Instant getActiveSlot() { return activeSlot; }
    public Instant getCancelledAt() { return cancelledAt; }

    public void cancelForFutureDomainService(Instant cancelledAt) {
        if (status == AppointmentStatus.CANCELLED) return;
        status = AppointmentStatus.CANCELLED;
        activeSlot = null;
        this.cancelledAt = cancelledAt;
    }

    public boolean confirmForFutureDomainService() {
        if (status == AppointmentStatus.CANCELLED) return false;
        if (status == AppointmentStatus.CONFIRMED) return false;
        status = AppointmentStatus.CONFIRMED;
        activeSlot = startAt;
        return true;
    }
}
