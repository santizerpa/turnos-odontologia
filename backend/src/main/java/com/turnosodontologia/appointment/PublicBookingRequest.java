package com.turnosodontologia.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record PublicBookingRequest(@NotBlank @Size(max = 100) String nombre,
                                   @NotBlank @Size(max = 100) String apellido,
                                   String dni,
                                   String email,
                                   @NotNull Instant startAt) {
}
