package com.turnosodontologia.schedule;

import com.turnosodontologia.schedule.config.ScheduleProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SchedulePropertiesTests {
    @Autowired ScheduleProperties properties;
    @Autowired ApplicationContext context;

    @Test
    void bindsScheduleDefaultsAndClock() {
        assertEquals("America/Argentina/Buenos_Aires", properties.getTimezone());
        assertEquals(30, properties.getSlotDurationMinutes());
        assertEquals(24, properties.getPublicWindow().minNoticeHours());
        assertEquals(30, properties.getPublicWindow().maxDays());
        assertEquals(10, properties.getDefaultWindows().size());
        assertNotNull(context.getBean(Clock.class));
    }
}
