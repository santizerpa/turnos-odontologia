package com.turnosodontologia.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentTokenRepository extends JpaRepository<AppointmentToken, UUID> {
    Optional<AppointmentToken> findByTokenHash(String tokenHash);
    Optional<AppointmentToken> findByAppointmentId(UUID appointmentId);
}
