package com.turnosodontologia.waiting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface WaitingListEntryRepository extends JpaRepository<WaitingListEntry, UUID> {}
