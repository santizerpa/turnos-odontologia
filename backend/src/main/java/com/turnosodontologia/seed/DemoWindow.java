package com.turnosodontologia.seed;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Franja semanal usada para calcular los turnos de demo. Es independiente de la
 * entidad persistida para que el planificador sea puro y determinista.
 */
public record DemoWindow(DayOfWeek day, LocalTime start, LocalTime end) {
    public DemoWindow {
        if (day == null || start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Franja de demo inválida");
        }
    }
}
