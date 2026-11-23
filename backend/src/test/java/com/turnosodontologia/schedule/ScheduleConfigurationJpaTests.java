package com.turnosodontologia.schedule;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ScheduleConfigurationJpaTests {
    @Autowired DentistRepository dentists;
    @Autowired ScheduleConfigurationRepository configurations;
    @Autowired EntityManager entityManager;

    @Test
    void mapsWindowsUniqueDentistAndSoftDelete() {
        Dentist dentist = dentists.save(new Dentist("agenda.jpa@odontologia.invalid", "hash", "Agenda", "Ficticia"));
        ScheduleConfiguration saved = configurations.saveAndFlush(new ScheduleConfiguration(dentist, List.of(
                new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(19, 0)),
                new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))));

        ScheduleConfiguration reloaded = configurations.findByDentistId(dentist.getId()).orElseThrow();
        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(14, 0)),
                reloaded.getWindows().stream().map(ScheduleWindow::getStartTime).toList());

        reloaded.markDeleted(Instant.parse("2026-11-23T12:00:00Z"));
        configurations.saveAndFlush(reloaded);
        ScheduleConfiguration deleted = configurations.findById(saved.getId()).orElseThrow();
        assertTrue(deleted.isDeleted());
    }

    @Test
    void rejectsDuplicateConfigurationForOneDentist() {
        Dentist dentist = dentists.save(new Dentist("agenda.unique@odontologia.invalid", "hash", "Agenda", "Unica"));
        configurations.saveAndFlush(new ScheduleConfiguration(dentist,
                List.of(new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> configurations.saveAndFlush(new ScheduleConfiguration(dentist,
                        List.of(new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(19, 0))))));
    }
}
