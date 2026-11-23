package com.turnosodontologia.schedule;

import com.turnosodontologia.schedule.config.ScheduleProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PublicBookingWindowTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void exactLimitsAndValuesFromProperties() {
        ScheduleProperties properties = new ScheduleProperties();
        properties.setTimezone("America/Argentina/Buenos_Aires");
        properties.setPublicWindow(new ScheduleProperties.PublicWindow(24, 30));
        PublicBookingWindow window = new PublicBookingWindow(properties, clock);

        Instant now = clock.instant();
        assertDoesNotThrow(() -> window.validate(now.plusSeconds(24 * 3600), now.plusSeconds(24 * 3600 + 3600), clock));
        assertDoesNotThrow(() -> window.validate(now.plusSeconds(24 * 3600), now.plusSeconds(30L * 86400), clock));

        assertThrows(IllegalArgumentException.class,
                () -> window.validate(now.plusSeconds(24 * 3600 - 1), now.plusSeconds(30L * 86400), clock));
        assertThrows(IllegalArgumentException.class,
                () -> window.validate(now.plusSeconds(24 * 3600), now.plusSeconds(30L * 86400 + 1), clock));
        assertThrows(IllegalArgumentException.class,
                () -> window.validate(now.plusSeconds(24 * 3600), now.plusSeconds(24 * 3600), clock));
        assertThrows(IllegalArgumentException.class,
                () -> window.validate(now.plusSeconds(25 * 3600), now.plusSeconds(24 * 3600), clock));
        assertEquals(24, window.minNoticeHours());
        assertEquals(30, window.maxDays());
    }
}
