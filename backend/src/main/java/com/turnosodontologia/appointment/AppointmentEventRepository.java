package com.turnosodontologia.appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AppointmentEventRepository extends JpaRepository<AppointmentEvent, UUID> {}
