package com.turnosodontologia.waiting;

public class InvalidWaitingListDataException extends RuntimeException {
    public InvalidWaitingListDataException() {
        super("Datos de inscripción inválidos.");
    }
}
