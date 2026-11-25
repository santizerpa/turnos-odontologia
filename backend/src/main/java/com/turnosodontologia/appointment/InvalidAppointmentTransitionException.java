package com.turnosodontologia.appointment;

public class InvalidAppointmentTransitionException extends IllegalStateException {
    public InvalidAppointmentTransitionException() {
        super("Transición de estado inválida");
    }
}
