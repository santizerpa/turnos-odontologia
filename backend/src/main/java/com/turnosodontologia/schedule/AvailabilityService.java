package com.turnosodontologia.schedule;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.AppointmentStatus;
import com.turnosodontologia.schedule.domain.ScheduleCalendar;
import com.turnosodontologia.schedule.domain.ScheduleSlot;
import com.turnosodontologia.schedule.domain.ScheduleWindow;
import com.turnosodontologia.schedule.domain.WeeklySchedule;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AvailabilityService {
    private final ScheduleConfigurationRepository configurations;
    private final ScheduleBlockRepository blocks;
    private final AppointmentRepository appointments;
    private final ZoneId zone;

    public AvailabilityService(ScheduleConfigurationRepository configurations,
                               ScheduleBlockRepository blocks,
                               AppointmentRepository appointments,
                               ZoneId scheduleZoneId) {
        this.configurations = configurations;
        this.blocks = blocks;
        this.appointments = appointments;
        this.zone = scheduleZoneId;
    }

    public List<ScheduleSlot> freeSlots(UUID dentistId, Instant from, Instant to) {
        if (dentistId == null || from == null || to == null || !to.isAfter(from)) {
            throw new IllegalArgumentException("Rango de disponibilidad inválido");
        }
        ScheduleConfiguration configuration = configurations.findByDentistId(dentistId)
                .orElseThrow(() -> new IllegalArgumentException("Configuración de agenda inexistente"));

        ScheduleCalendar calendar = new ScheduleCalendar(weeklySchedule(configuration), zone, 30);
        ZonedDateTime fromLocal = from.atZone(zone);
        ZonedDateTime toLocal = to.atZone(zone);
        List<ScheduleSlot> slots = calendar.slotsBetween(fromLocal.toLocalDate(), toLocal.toLocalDate()).stream()
                .filter(slot -> !slot.start().isBefore(from) && !slot.end().isAfter(to))
                .toList();

        List<ScheduleBlock> activeBlocks = blocks.findByDentistIdAndDeletedAtIsNullAndStartAtLessThanAndEndAtGreaterThan(dentistId, to, from);
        List<Appointment> activeAppointments = appointments
                .findByDentistIdAndDeletedAtIsNullAndActiveSlotIsNotNullAndStartAtGreaterThanEqualAndStartAtLessThan(dentistId, from.minusSeconds(1800), to);

        List<ScheduleSlot> free = new ArrayList<>();
        for (ScheduleSlot slot : slots) {
            boolean blocked = activeBlocks.stream().anyMatch(block ->
                    block.getStartAt().isBefore(slot.end()) && block.getEndAt().isAfter(slot.start()));
            boolean occupied = activeAppointments.stream().anyMatch(appointment -> {
                if (appointment.getStatus() != AppointmentStatus.PENDING
                        && appointment.getStatus() != AppointmentStatus.CONFIRMED) return false;
                Instant appointmentEnd = appointment.getStartAt().plusSeconds(appointment.getDurationMinutes() * 60L);
                return appointment.getStartAt().isBefore(slot.end()) && appointmentEnd.isAfter(slot.start());
            });
            if (!blocked && !occupied) free.add(slot);
        }
        return List.copyOf(free);
    }

    public boolean isOpenSlot(UUID dentistId, Instant start) {
        if (dentistId == null || start == null) return false;
        ZonedDateTime localStart = start.atZone(zone);
        Instant end = start.plusSeconds(30 * 60L);
        try {
            for (ScheduleSlot slot : freeSlots(dentistId, start, end)) {
                if (slot.start().equals(start)) return true;
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return false;
    }

    private WeeklySchedule weeklySchedule(ScheduleConfiguration configuration) {
        Map<DayOfWeek, List<ScheduleWindow>> map = new EnumMap<>(DayOfWeek.class);
        for (com.turnosodontologia.schedule.ScheduleWindow persisted : configuration.getWindows()) {
            map.computeIfAbsent(persisted.getDayOfWeek(), ignored -> new ArrayList<>())
                    .add(new ScheduleWindow(persisted.getDayOfWeek(), persisted.getStartTime(), persisted.getEndTime()));
        }
        return new WeeklySchedule(map);
    }
}
