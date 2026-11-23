package com.turnosodontologia.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByDentistIdAndDeletedAtIsNullAndActiveSlotIsNotNullAndStartAtGreaterThanEqualAndStartAtLessThan(
            UUID dentistId, Instant lookbackFrom, Instant to);
    boolean existsByDentistIdAndActiveSlot(UUID dentistId, Instant activeSlot);
}
