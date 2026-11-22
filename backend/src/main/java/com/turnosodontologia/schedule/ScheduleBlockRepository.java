package com.turnosodontologia.schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, UUID> {}
