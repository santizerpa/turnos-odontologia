package com.turnosodontologia.schedule.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleCoreTests {
    @Test
    void timeRangeRejectsEmptyAndHalfOpenTouches() {
        assertThrows(IllegalArgumentException.class, () -> new TimeRange(Instant.parse("2026-11-23T10:00:00Z"), Instant.parse("2026-11-23T10:00:00Z")));
        assertThrows(IllegalArgumentException.class, () -> new TimeRange(Instant.parse("2026-11-23T11:00:00Z"), Instant.parse("2026-11-23T10:00:00Z")));

        TimeRange first = new TimeRange(Instant.parse("2026-11-23T09:00:00Z"), Instant.parse("2026-11-23T10:00:00Z"));
        TimeRange touching = new TimeRange(Instant.parse("2026-11-23T10:00:00Z"), Instant.parse("2026-11-23T11:00:00Z"));
        assertFalse(first.overlaps(touching));
        assertTrue(first.contains(Instant.parse("2026-11-23T09:30:00Z")));
        assertFalse(first.contains(Instant.parse("2026-11-23T10:00:00Z")));
    }

    @Test
    void weeklyScheduleNormalizesAndRejectsInvalidWindows() {
        EnumMap<DayOfWeek, List<ScheduleWindow>> map = new EnumMap<>(DayOfWeek.class);
        map.put(DayOfWeek.MONDAY, List.of(
                new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(19, 0)),
                new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))));
        map.put(DayOfWeek.SUNDAY, List.of());

        WeeklySchedule schedule = new WeeklySchedule(map);
        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(14, 0)),
                schedule.windowsFor(DayOfWeek.MONDAY).stream().map(ScheduleWindow::startTime).toList());
        assertTrue(schedule.windowsFor(DayOfWeek.SUNDAY).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 20)));
        assertThrows(IllegalArgumentException.class, () -> new WeeklySchedule(new EnumMap<>(map) {{
            put(DayOfWeek.MONDAY, List.of(new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                    new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(10, 30), LocalTime.of(12, 0))));
        }}));
    }

    @Test
    void calendarUsesDefaultGridAndOnlyCompleteSlots() {
        WeeklySchedule schedule = WeeklySchedule.defaults();
        ScheduleCalendar calendar = new ScheduleCalendar(schedule, ZoneId.of("America/Argentina/Buenos_Aires"), 30);

        assertEquals(18, calendar.slotsFor(ZoneId.of("America/Argentina/Buenos_Aires"), java.time.LocalDate.of(2026, 11, 23), 30).size());
        assertEquals(90, calendar.slotsBetween(java.time.LocalDate.of(2026, 11, 23), java.time.LocalDate.of(2026, 11, 29)).size());
        assertTrue(calendar.slotsFor(ZoneId.of("America/Argentina/Buenos_Aires"), java.time.LocalDate.of(2026, 11, 23), 30).stream()
                .anyMatch(slot -> slot.start().atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalTime().equals(LocalTime.of(18, 30))));

        EnumMap<DayOfWeek, List<ScheduleWindow>> map = new EnumMap<>(DayOfWeek.class);
        map.put(DayOfWeek.MONDAY, List.of(new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(9, 15), LocalTime.of(10, 0))));
        ScheduleCalendar desaligned = new ScheduleCalendar(new WeeklySchedule(map), ZoneId.of("America/Argentina/Buenos_Aires"), 30);
        assertEquals(1, desaligned.slotsFor(ZoneId.of("America/Argentina/Buenos_Aires"), java.time.LocalDate.of(2026, 11, 23), 30).size());

        assertTrue(desaligned.slotsFor(ZoneId.of("America/Argentina/Buenos_Aires"), java.time.LocalDate.of(2026, 11, 23), 30).get(0).start()
                .atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalTime().equals(LocalTime.of(9, 15)));
    }

    @Test
    void fixedTimezoneKeepsArgentinaOffsetInJanuaryAndJuly() {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        ScheduleCalendar calendar = new ScheduleCalendar(WeeklySchedule.defaults(), zone, 30);

        var january = calendar.slotsFor(zone, java.time.LocalDate.of(2026, 1, 5), 30).get(0).start();
        var july = calendar.slotsFor(zone, java.time.LocalDate.of(2026, 7, 6), 30).get(0).start();
        assertEquals("-03:00", january.atZone(zone).getOffset().toString());
        assertEquals("-03:00", july.atZone(zone).getOffset().toString());
    }
}
