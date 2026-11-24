package com.turnosodontologia.waiting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WaitingListEntryRepository extends JpaRepository<WaitingListEntry, UUID> {
    List<WaitingListEntry> findAllByOrderByCreatedAtAscIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT entry FROM WaitingListEntry entry
        WHERE entry.status = :status
          AND (entry.requestedDateFrom IS NULL OR entry.requestedDateFrom <= :releasedDate)
          AND (entry.requestedDateTo IS NULL OR entry.requestedDateTo >= :releasedDate)
        ORDER BY entry.createdAt ASC, entry.id ASC
        """)
    List<WaitingListEntry> findEligibleWaitingEntriesForUpdate(
        @Param("status") WaitingListStatus status,
        @Param("releasedDate") LocalDate releasedDate);
}
