package com.turnosodontologia.schedule.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScheduleCalendar {
    private final WeeklySchedule schedule;
    private final ZoneId zone;
    private final int slotDurationMinutes;

    public ScheduleCalendar(WeeklySchedule schedule, ZoneId zone, int slotDurationMinutes) {
        if (schedule == null || zone == null || slotDurationMinutes < 30) {
            throw new IllegalArgumentException("Configuración de grilla inválida");
        }
        this.schedule = schedule;
        this.zone = zone;
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public List<ScheduleSlot> slotsFor(ZoneId ignored, LocalDate date, int ignoredDuration) {
        List<ScheduleSlot> slots = new ArrayList<>();
        for (ScheduleWindow window : schedule.windowsFor(date.getDayOfWeek())) {
            var start = window.startTime();
            while (!start.plusMinutes(slotDurationMinutes).isAfter(window.endTime())) {
                ZonedDateTime startAt = ZonedDateTime.of(LocalDateTime.of(date, start), zone);
                ZonedDateTime endAt = startAt.plusMinutes(slotDurationMinutes);
                slots.add(new ScheduleSlot(startAt.toInstant(), endAt.toInstant()));
                start = start.plusMinutes(slotDurationMinutes);
            }
        }
        return slots;
    }

    public List<ScheduleSlot> slotsBetween(LocalDate from, LocalDate to) {
        List<ScheduleSlot> slots = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            slots.addAll(slotsFor(zone, date, slotDurationMinutes));
        }
        slots.sort(Comparator.comparing(ScheduleSlot::start));
        return List.copyOf(slots);
    }
}
