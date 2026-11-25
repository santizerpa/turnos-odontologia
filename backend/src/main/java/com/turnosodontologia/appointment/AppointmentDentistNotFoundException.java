package com.turnosodontologia.appointment;

public class AppointmentDentistNotFoundException extends IllegalArgumentException {
    public AppointmentDentistNotFoundException() {
        super("Odontólogo inexistente");
    }
}
