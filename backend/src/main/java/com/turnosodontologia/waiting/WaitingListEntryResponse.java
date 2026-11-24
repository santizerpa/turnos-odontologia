package com.turnosodontologia.waiting;

import java.time.LocalDate;
import java.util.UUID;

public record WaitingListEntryResponse(
    UUID id,
    WaitingListStatus status,
    LocalDate requestedDateFrom,
    LocalDate requestedDateTo
) {
    public static WaitingListEntryResponse from(WaitingListEntry entry) {
        return new WaitingListEntryResponse(entry.getId(), entry.getStatus(),
            entry.getRequestedDateFrom(), entry.getRequestedDateTo());
    }
}
