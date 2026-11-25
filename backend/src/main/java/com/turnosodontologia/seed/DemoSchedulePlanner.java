package com.turnosodontologia.seed;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Calcula días y horarios deterministas para el conjunto de demo a partir del
 * reloj de agenda, la zona configurada y las franjas vigentes.
 *
 * <p>Los turnos activos se ubican entre 2 y 29 días después de la fecha efectiva:
 * así quedan siempre dentro de la ventana inclusiva de 24 horas a 30 días,
 * independientemente de la hora del día en que se ejecute el seed.</p>
 */
public final class DemoSchedulePlanner {
    public static final int SLOT_MINUTES = 30;
    private static final int FIRST_OFFSET_DAYS = 2;
    private static final int LAST_OFFSET_DAYS = 29;
    private static final int MINIMUM_DAYS = 4;

    public DemoSchedulePlan plan(Instant now, ZoneId zone, List<DemoWindow> windows) {
        if (now == null || zone == null) {
            throw new IllegalArgumentException("Reloj y zona requeridos para el plan de demo");
        }
        if (windows == null || windows.isEmpty()) {
            throw new DemoSeedException("No hay franjas configuradas para el conjunto de demo.");
        }

        LocalDate effectiveDate = now.atZone(zone).toLocalDate();
        Map<LocalDate, List<Instant>> slotsByDate = new TreeMap<>();
        for (int offset = FIRST_OFFSET_DAYS; offset <= LAST_OFFSET_DAYS; offset++) {
            LocalDate date = effectiveDate.plusDays(offset);
            List<Instant> slots = slotsFor(date, zone, windows).stream()
                .filter(start -> isWithinWindow(start, now))
                .toList();
            if (!slots.isEmpty()) {
                slotsByDate.put(date, slots);
            }
        }

        List<LocalDate> days = new ArrayList<>(slotsByDate.keySet());
        if (days.size() < MINIMUM_DAYS) {
            throw insufficientSlots();
        }
        List<Instant> firstDaySlots = slotsByDate.get(days.get(0));
        if (firstDaySlots.size() < 2) {
            throw insufficientSlots();
        }

        Instant firstConfirmedStart = firstDaySlots.get(0);
        Instant blockStart = firstDaySlots.get(1);
        Instant blockEnd = blockStart.plusSeconds(SLOT_MINUTES * 60L);
        Instant secondConfirmedStart = slotsByDate.get(days.get(1)).get(0);
        Instant pendingStart = slotsByDate.get(days.get(2)).get(0);
        Instant cancelledStart = slotsByDate.get(days.get(3)).get(0);

        return new DemoSchedulePlan(effectiveDate, blockStart, blockEnd,
            firstConfirmedStart, secondConfirmedStart, pendingStart, cancelledStart);
    }

    public static boolean isWithinWindow(Instant start, Instant now) {
        Instant earliest = now.plusSeconds(24 * 3600L);
        Instant latest = now.plusSeconds(30 * 86_400L);
        return !start.isBefore(earliest) && !start.isAfter(latest);
    }

    private List<Instant> slotsFor(LocalDate date, ZoneId zone, List<DemoWindow> windows) {
        TreeSet<Instant> slots = new TreeSet<>();
        for (DemoWindow window : windows) {
            if (window.day() != date.getDayOfWeek()) continue;
            LocalTime start = window.start();
            while (!start.plusMinutes(SLOT_MINUTES).isAfter(window.end())) {
                ZonedDateTime startAt = ZonedDateTime.of(LocalDateTime.of(date, start), zone);
                slots.add(startAt.toInstant());
                start = start.plusMinutes(SLOT_MINUTES);
            }
        }
        return List.copyOf(slots);
    }

    private DemoSeedException insufficientSlots() {
        return new DemoSeedException(
            "No hay slots válidos suficientes para el conjunto de demo: se necesitan al menos "
                + MINIMUM_DAYS + " días con franjas y 2 turnos consecutivos en el primero. "
                + "Revisá la agenda configurada y la ventana de 24 horas a 30 días.");
    }
}
