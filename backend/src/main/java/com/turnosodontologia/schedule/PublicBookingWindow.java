package com.turnosodontologia.schedule;

import com.turnosodontologia.schedule.config.ScheduleProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class PublicBookingWindow {
    private final int minNoticeHours;
    private final int maxDays;

    public PublicBookingWindow(ScheduleProperties properties, Clock scheduleClock) {
        if (properties == null || properties.getPublicWindow() == null) {
            throw new IllegalArgumentException("Ventana pública requerida");
        }
        this.minNoticeHours = properties.getPublicWindow().minNoticeHours();
        this.maxDays = properties.getPublicWindow().maxDays();
        if (this.minNoticeHours < 0 || this.maxDays < 0) {
            throw new IllegalArgumentException("Ventana pública inválida");
        }
    }

    public Instant earliest(Clock clock) {
        if (clock == null) throw new IllegalArgumentException("Reloj requerido");
        return clock.instant().plusSeconds(minNoticeHours * 3600L);
    }

    public Instant latest(Clock clock) {
        if (clock == null) throw new IllegalArgumentException("Reloj requerido");
        return clock.instant().plusSeconds(maxDays * 86400L);
    }

    public boolean isWithin(Instant startAt, Clock clock) {
        if (startAt == null) throw new IllegalArgumentException("Inicio requerido");
        return !startAt.isBefore(earliest(clock)) && !startAt.isAfter(latest(clock));
    }

    public void validate(Instant from, Instant to, Clock clock) {
        if (from == null || to == null) throw new IllegalArgumentException("Rango público requerido");
        if (!to.isAfter(from)) throw new IllegalArgumentException("Rango vacío o invertido");
        Instant earliest = earliest(clock);
        Instant latest = latest(clock);
        if (from.isBefore(earliest)) throw new IllegalArgumentException("El inicio debe ser al menos 24 horas desde ahora");
        if (to.isAfter(latest)) throw new IllegalArgumentException("El fin no puede superar 30 días desde ahora");
    }

    public int minNoticeHours() { return minNoticeHours; }
    public int maxDays() { return maxDays; }
}
