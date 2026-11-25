package com.turnosodontologia.appointment;

public class AppointmentPatientNotFoundException extends IllegalArgumentException {
    public AppointmentPatientNotFoundException() {
        super("Paciente inexistente");
    }
}
