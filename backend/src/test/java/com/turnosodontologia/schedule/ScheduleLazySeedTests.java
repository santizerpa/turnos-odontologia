package com.turnosodontologia.schedule;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.schedule.config.ScheduleProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(ScheduleConfigurationService.class)
@EnableConfigurationProperties(ScheduleProperties.class)
class ScheduleLazySeedTests {
    @Autowired DentistRepository dentists;
    @Autowired ScheduleConfigurationRepository configurations;
    @Autowired ScheduleConfigurationService service;

    @Test
    void firstReadSeedsOneConfigurationAndSecondIsIdempotent() {
        Dentist dentist = dentists.save(new Dentist("seed.one@odontologia.invalid", "hash", "Uno", "Ficticio"));
        ScheduleConfiguration first = service.getOrCreate(dentist);
        ScheduleConfiguration second = service.getOrCreate(dentist);

        assertEquals(first.getId(), second.getId());
        assertEquals(10, configurations.findByDentistId(dentist.getId()).orElseThrow().getWindows().size());
        assertEquals(1, configurations.count());
    }

    @Test
    void getIsNotMarkedReadOnlyBecauseItMayCreateTheConfiguration() throws NoSuchMethodException {
        Method get = ScheduleConfigurationService.class.getMethod("get", Dentist.class);

        Transactional transaction = AnnotatedElementUtils.findMergedAnnotation(get, Transactional.class);

        assertNotNull(transaction);
        assertFalse(transaction.readOnly());
    }

    @Test
    void configurationsAreSeparatedPerDentist() {
        Dentist first = dentists.save(new Dentist("seed.two@odontologia.invalid", "hash", "Dos", "Ficticio"));
        Dentist second = dentists.save(new Dentist("seed.three@odontologia.invalid", "hash", "Tres", "Ficticio"));
        service.getOrCreate(first);
        service.getOrCreate(second);

        assertEquals(2, configurations.count());
        assertNotEquals(configurations.findByDentistId(first.getId()).orElseThrow().getId(),
                configurations.findByDentistId(second.getId()).orElseThrow().getId());
    }
}
