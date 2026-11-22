package com.turnosodontologia.patient;

public class InvalidPatientDataException extends RuntimeException {
    public InvalidPatientDataException() {
        super("Datos de paciente inválidos.");
    }
}
