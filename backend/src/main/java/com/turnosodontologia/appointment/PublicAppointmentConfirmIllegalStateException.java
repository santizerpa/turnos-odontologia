package com.turnosodontologia.appointment;

public class PublicAppointmentConfirmIllegalStateException extends IllegalStateException {
    public PublicAppointmentConfirmIllegalStateException(IllegalStateException cause) {
        super(cause.getMessage(), cause);
    }
}
