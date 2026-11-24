package com.turnosodontologia.waiting;

import java.time.LocalDate;
import java.util.UUID;

public record WaitingListAdminResponse(
    UUID id,
    String nombre,
    String apellido,
    String email,
    LocalDate requestedDateFrom,
    LocalDate requestedDateTo,
    WaitingListStatus status
) {
    public static WaitingListAdminResponse from(WaitingListEntry entry) {
        return new WaitingListAdminResponse(entry.getId(), entry.getPatient().getFirstName(),
            entry.getPatient().getLastName(), entry.getPatient().getEmail(), entry.getRequestedDateFrom(),
            entry.getRequestedDateTo(), entry.getStatus());
    }
}
