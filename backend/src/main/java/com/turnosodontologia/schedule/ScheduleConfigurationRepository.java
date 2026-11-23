package com.turnosodontologia.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScheduleConfigurationRepository extends JpaRepository<ScheduleConfiguration, UUID> {
    Optional<ScheduleConfiguration> findByDentistId(UUID dentistId);
}
