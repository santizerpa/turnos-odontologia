package com.turnosodontologia.schedule.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ScheduleClockConfiguration {
    @Bean
    public Clock scheduleClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ZoneId scheduleZoneId(ScheduleProperties properties) {
        return ZoneId.of(properties.getTimezone());
    }
}
