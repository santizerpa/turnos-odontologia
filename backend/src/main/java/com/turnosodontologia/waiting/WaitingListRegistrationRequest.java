package com.turnosodontologia.waiting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record WaitingListRegistrationRequest(
    String dni,
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 100) String apellido,
    String email,
    LocalDate requestedDateFrom,
    LocalDate requestedDateTo
) {
}
