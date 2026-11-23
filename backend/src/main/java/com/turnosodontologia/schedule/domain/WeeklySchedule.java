package com.turnosodontologia.schedule.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WeeklySchedule {
    private final Map<DayOfWeek, List<ScheduleWindow>> windows;

    public WeeklySchedule(Map<DayOfWeek, List<ScheduleWindow>> source) {
        windows = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            List<ScheduleWindow> dayWindows = new ArrayList<>(source.getOrDefault(day, List.of()));
            dayWindows.sort(java.util.Comparator.comparing(ScheduleWindow::startTime)
                    .thenComparing(ScheduleWindow::endTime));
            for (int i = 0; i < dayWindows.size(); i++) {
                ScheduleWindow window = dayWindows.get(i);
                if (window.dayOfWeek() != day) {
                    throw new IllegalArgumentException("Franja asignada al día incorrecto");
                }
                if (i > 0) {
                    ScheduleWindow previous = dayWindows.get(i - 1);
                    if (!window.startTime().isAfter(previous.endTime())) {
                        throw new IllegalArgumentException("Franjas solapadas");
                    }
                }
            }
            windows.put(day, List.copyOf(dayWindows));
        }
    }

    public static WeeklySchedule defaults() {
        EnumMap<DayOfWeek, List<ScheduleWindow>> map = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            map.put(day, List.of(
                    new ScheduleWindow(day, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                    new ScheduleWindow(day, LocalTime.of(14, 0), LocalTime.of(19, 0))));
        }
        return new WeeklySchedule(map);
    }

    public List<ScheduleWindow> windowsFor(DayOfWeek day) {
        return windows.getOrDefault(day, List.of());
    }
}
