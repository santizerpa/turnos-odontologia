package com.turnosodontologia.appointment;

public class InvalidAppointmentRangeException extends IllegalArgumentException {
    public InvalidAppointmentRangeException() {
        super("Rango de agenda inválido");
    }
}
