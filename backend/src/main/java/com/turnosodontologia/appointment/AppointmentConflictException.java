package com.turnosodontologia.appointment;

public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException() {
        super("La franja solicitada ya está ocupada");
    }
}
