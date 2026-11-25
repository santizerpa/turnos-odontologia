package com.turnosodontologia.schedule;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.schedule.config.ScheduleProperties;
import com.turnosodontologia.schedule.dto.ScheduleConfigurationResponse;
import com.turnosodontologia.schedule.dto.ScheduleDayResponse;
import com.turnosodontologia.schedule.dto.SchedulePatchRequest;
import com.turnosodontologia.schedule.dto.SchedulePublicWindowResponse;
import com.turnosodontologia.schedule.dto.ScheduleWindowRequest;
import com.turnosodontologia.schedule.dto.ScheduleWindowResponse;
import com.turnosodontologia.schedule.error.ScheduleValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleConfigurationService {
    private final ScheduleConfigurationRepository configurations;
    private final ScheduleProperties properties;

    public ScheduleConfigurationService(ScheduleConfigurationRepository configurations, ScheduleProperties properties) {
        this.configurations = configurations;
        this.properties = properties;
    }

    @Transactional
    public ScheduleConfiguration getOrCreate(Dentist dentist) {
        var existing = configurations.findByDentistId(dentist.getId());
        if (existing.isPresent()) return existing.get();

        List<ScheduleWindow> windows = new ArrayList<>();
        for (ScheduleProperties.DefaultWindow window : properties.getDefaultWindows()) {
            windows.add(new ScheduleWindow(DayOfWeek.valueOf(window.day()),
                    LocalTime.parse(window.start()), LocalTime.parse(window.end())));
        }

        try {
            return configurations.saveAndFlush(new ScheduleConfiguration(dentist, windows));
        } catch (DataIntegrityViolationException ignored) {
            return configurations.findByDentistId(dentist.getId()).orElseThrow();
        }
    }

    @Transactional
    public ScheduleConfigurationResponse get(Dentist dentist) {
        ScheduleConfiguration configuration = getOrCreate(dentist);
        return response(configuration);
    }

    @Transactional
    public ScheduleConfigurationResponse patch(Dentist dentist, SchedulePatchRequest request) {
        if (request == null || request.days() == null) {
            throw new ScheduleValidationException("Los días de la agenda son obligatorios");
        }
        if (request.timezone() != null && !properties.getTimezone().equals(request.timezone())) {
            throw new ScheduleValidationException("La timezone no coincide con la configuración fija");
        }

        EnumSet<DayOfWeek> allowed = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        Map<DayOfWeek, List<ScheduleWindow>> validated = new EnumMap<>(DayOfWeek.class);
        for (com.turnosodontologia.schedule.dto.ScheduleDayRequest day : request.days()) {
            if (day == null || day.day() == null) throw new ScheduleValidationException("Día inválido");
            DayOfWeek dayOfWeek;
            try { dayOfWeek = DayOfWeek.valueOf(day.day()); }
            catch (IllegalArgumentException exception) { throw new ScheduleValidationException("Día no permitido: " + day.day()); }
            if (!allowed.contains(dayOfWeek)) throw new ScheduleValidationException("Día no permitido: " + day.day());
            if (validated.containsKey(dayOfWeek)) throw new ScheduleValidationException("Día repetido: " + day.day());

            List<ScheduleWindow> windows = new ArrayList<>();
            List<ScheduleWindowRequest> source = day.windows();
            if (source == null) throw new ScheduleValidationException("Las franjas son obligatorias; use una lista vacía para cerrar el día");
            for (ScheduleWindowRequest window : source) {
                if (window == null || window.start() == null || window.end() == null) {
                    throw new ScheduleValidationException("Franja inválida para " + day.day());
                }
                LocalTime start;
                LocalTime end;
                try { start = LocalTime.parse(window.start()); end = LocalTime.parse(window.end()); }
                catch (RuntimeException exception) { throw new ScheduleValidationException("Franja mal formada para " + day.day()); }
                if (!end.isAfter(start)) throw new ScheduleValidationException("Franja invertida en " + day.day());
                if (java.time.Duration.between(start, end).toMinutes() < properties.getSlotDurationMinutes()) {
                    throw new ScheduleValidationException("Franja menor a " + properties.getSlotDurationMinutes() + " minutos en " + day.day());
                }
                windows.add(new ScheduleWindow(dayOfWeek, start, end));
            }
            windows.sort(java.util.Comparator.comparing(ScheduleWindow::getStartTime));
            for (int i = 1; i < windows.size(); i++) {
                if (!windows.get(i).getStartTime().isAfter(windows.get(i - 1).getEndTime())
                        && !windows.get(i).getStartTime().equals(windows.get(i - 1).getEndTime())) {
                    throw new ScheduleValidationException("Franjas solapadas en " + day.day());
                }
            }
            validated.put(dayOfWeek, windows);
        }
        if (validated.keySet().size() != allowed.size()) {
            throw new ScheduleValidationException("Debe incluir exactamente los días MONDAY a FRIDAY una vez");
        }

        ScheduleConfiguration configuration = getOrCreate(dentist);
        List<ScheduleWindow> replacement = new ArrayList<>();
        for (Map.Entry<DayOfWeek, List<ScheduleWindow>> entry : validated.entrySet()) replacement.addAll(entry.getValue());
        configuration.replaceWindows(replacement);
        configurations.save(configuration);
        return response(configuration);
    }

    private ScheduleConfigurationResponse response(ScheduleConfiguration configuration) {
        Map<DayOfWeek, List<ScheduleWindow>> byDay = new EnumMap<>(DayOfWeek.class);
        for (ScheduleWindow window : configuration.getWindows()) {
            byDay.computeIfAbsent(window.getDayOfWeek(), ignored -> new ArrayList<>())
                    .add(window);
        }
        List<ScheduleDayResponse> days = new ArrayList<>();
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            List<ScheduleWindowResponse> windows = byDay.getOrDefault(day, List.of()).stream()
                    .map(window -> new ScheduleWindowResponse(window.getStartTime().toString(), window.getEndTime().toString()))
                    .toList();
            days.add(new ScheduleDayResponse(day, windows));
        }
        return new ScheduleConfigurationResponse(properties.getTimezone(), properties.getSlotDurationMinutes(),
                new SchedulePublicWindowResponse(properties.getPublicWindow().minNoticeHours(),
                        properties.getPublicWindow().maxDays(), properties.getTimezone()), days);
    }
}
