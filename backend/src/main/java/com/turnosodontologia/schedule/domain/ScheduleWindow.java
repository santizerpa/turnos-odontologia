package com.turnosodontologia.schedule.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleWindow(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    public ScheduleWindow {
        if (dayOfWeek == null || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Franja inválida");
        }
        if (java.time.Duration.between(startTime, endTime).toMinutes() < 30) {
            throw new IllegalArgumentException("La franja debe durar al menos 30 minutos");
        }
    }
}
