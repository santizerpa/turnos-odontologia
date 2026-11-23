package com.turnosodontologia.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, UUID> {
    List<ScheduleBlock> findByDentistIdAndDeletedAtIsNullAndStartAtLessThanAndEndAtGreaterThan(
            UUID dentistId, Instant to, Instant from);
}
