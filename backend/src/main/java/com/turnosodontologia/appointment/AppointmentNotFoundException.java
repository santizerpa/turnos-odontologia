package com.turnosodontologia.appointment;

import java.util.UUID;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(UUID id) {
        super("Turno no encontrado: " + id);
    }
}
