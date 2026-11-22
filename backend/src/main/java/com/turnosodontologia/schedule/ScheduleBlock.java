package com.turnosodontologia.schedule;

import com.turnosodontologia.common.AuditableEntity;
import com.turnosodontologia.dentist.Dentist;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_blocks", indexes = @Index(name = "idx_schedule_dentist", columnList = "dentist_id"))
public class ScheduleBlock extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dentist_id", nullable = false)
    private Dentist dentist;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(length = 255)
    private String reason;

    protected ScheduleBlock() {}

    public ScheduleBlock(Dentist dentist, Instant startAt, Instant endAt, String reason) {
        if (dentist == null || startAt == null || endAt == null || !endAt.isAfter(startAt)) throw new IllegalArgumentException("Rango de bloqueo inválido");
        this.dentist = dentist;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
    }

    public UUID getId() { return id; }
    public Dentist getDentist() { return dentist; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getReason() { return reason; }
}
