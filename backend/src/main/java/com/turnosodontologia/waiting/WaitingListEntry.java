package com.turnosodontologia.waiting;

import com.turnosodontologia.common.AuditableEntity;
import com.turnosodontologia.patient.Patient;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "waiting_list_entries", indexes = @Index(name = "idx_waiting_patient", columnList = "patient_id"))
public class WaitingListEntry extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "requested_date_from")
    private LocalDate requestedDateFrom;

    @Column(name = "requested_date_to")
    private LocalDate requestedDateTo;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitingListStatus status;

    protected WaitingListEntry() {}

    public WaitingListEntry(Patient patient, LocalDate from, LocalDate to, WaitingListStatus status) {
        if (patient == null) throw new IllegalArgumentException("Paciente requerido");
        this.patient = patient;
        this.requestedDateFrom = from;
        this.requestedDateTo = to;
        this.status = status;
    }

    public UUID getId() { return id; }
    public Patient getPatient() { return patient; }
    public LocalDate getRequestedDateFrom() { return requestedDateFrom; }
    public LocalDate getRequestedDateTo() { return requestedDateTo; }
    public WaitingListStatus getStatus() { return status; }

    public boolean markNotified() {
        if (status != WaitingListStatus.WAITING) return false;
        status = WaitingListStatus.NOTIFIED;
        return true;
    }
}
