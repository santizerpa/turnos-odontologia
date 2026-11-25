package com.turnosodontologia.appointment;

/** The dentist cannot confirm appointments; confirmation belongs to the patient's public token flow. */
public class DentistConfirmationNotAllowedException extends RuntimeException {
    public DentistConfirmationNotAllowedException() {
        super("La confirmación del turno la realiza el paciente");
    }
}
