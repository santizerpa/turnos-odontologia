package com.turnosodontologia.waiting;

import com.turnosodontologia.schedule.config.ScheduleProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class WaitingListSelectionService {

    private final WaitingListEntryRepository waitingEntries;
    private final ZoneId zoneId;

    public WaitingListSelectionService(WaitingListEntryRepository waitingEntries,
                                       ScheduleProperties scheduleProperties) {
        this.waitingEntries = waitingEntries;
        this.zoneId = ZoneId.of(scheduleProperties.getTimezone());
    }

    @Transactional
    public Optional<WaitingListEntry> selectFirstEligible(Instant releasedAt) {
        if (releasedAt == null) {
            throw new IllegalArgumentException("Instante liberado requerido.");
        }
        return selectFirstEligible(releasedAt.atZone(zoneId).toLocalDate(), null);
    }

    @Transactional
    public Optional<WaitingListEntry> selectFirstEligible(LocalDate releasedDate) {
        if (releasedDate == null) {
            throw new IllegalArgumentException("Fecha liberada requerida.");
        }
        return selectFirstEligible(releasedDate, null);
    }

    @Transactional
    public Optional<WaitingListEntry> selectFirstEligible(LocalDate releasedDate, UUID excludedPatientId) {
        if (releasedDate == null) {
            throw new IllegalArgumentException("Fecha liberada requerida.");
        }
        return waitingEntries.findEligibleWaitingEntriesForUpdate(WaitingListStatus.WAITING, releasedDate,
                excludedPatientId)
            .stream()
            .findFirst();
    }

    @Transactional
    public Optional<WaitingListEntry> selectFirstEligible(Instant releasedAt, UUID excludedPatientId) {
        if (releasedAt == null) {
            throw new IllegalArgumentException("Instante liberado requerido.");
        }
        return selectFirstEligible(releasedAt.atZone(zoneId).toLocalDate(), excludedPatientId);
    }
}
