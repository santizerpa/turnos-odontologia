package com.turnosodontologia.patient;

import java.util.UUID;

public record PatientResponse(UUID id, String dni, String nombre, String apellido, String email) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getDni(), patient.getFirstName(),
            patient.getLastName(), patient.getEmail());
    }
}
