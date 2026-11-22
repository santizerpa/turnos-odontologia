package com.turnosodontologia.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientRequest(String dni,
                             @NotBlank @Size(max = 100) String nombre,
                             @NotBlank @Size(max = 100) String apellido,
                             String email) {
}
