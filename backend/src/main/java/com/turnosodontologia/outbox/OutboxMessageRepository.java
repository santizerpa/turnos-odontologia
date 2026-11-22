package com.turnosodontologia.outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {}
