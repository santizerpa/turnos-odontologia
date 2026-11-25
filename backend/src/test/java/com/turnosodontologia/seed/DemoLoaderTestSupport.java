package com.turnosodontologia.seed;

import com.turnosodontologia.appointment.AppointmentEventRepository;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.AppointmentTokenRepository;
import com.turnosodontologia.appointment.AppointmentTokenService;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.outbox.OutboxMessageRepository;
import com.turnosodontologia.patient.PatientRepository;
import com.turnosodontologia.schedule.ScheduleBlockRepository;
import com.turnosodontologia.schedule.ScheduleConfigurationRepository;
import com.turnosodontologia.schedule.config.ScheduleProperties;
import com.turnosodontologia.waiting.WaitingListEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

abstract class DemoLoaderTestSupport {
    static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    static final String DENTIST_EMAIL = "demo.seed@odontologia.invalid";
    static final List<String> DEMO_DNIS = List.of(
        "99000101", "99000102", "99000103", "99000104", "99000105", "99000106", "99000107");

    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentEventRepository appointmentEvents;
    @Autowired AppointmentTokenRepository tokens;
    @Autowired ScheduleBlockRepository blocks;
    @Autowired ScheduleConfigurationRepository configurations;
    @Autowired WaitingListEntryRepository waitingEntries;
    @Autowired OutboxMessageRepository outbox;

    Dentist createDentist() {
        return dentists.saveAndFlush(new Dentist(DENTIST_EMAIL, "hash-ficticio", "Demo", "Odontologo"));
    }

    DemoDataLoader loader() {
        return loader(NOW);
    }

    DemoDataLoader loader(Instant now) {
        return new DemoDataLoader(dentists, patients, appointments, blocks, configurations,
            waitingEntries, outbox, new AppointmentTokenService(tokens, Clock.fixed(now, ZONE)),
            properties(), Clock.fixed(now, ZONE), ZONE);
    }

    ScheduleProperties properties() {
        ScheduleProperties properties = new ScheduleProperties();
        List<ScheduleProperties.DefaultWindow> windows = new ArrayList<>();
        for (DemoWindow window : defaultAgenda()) {
            windows.add(new ScheduleProperties.DefaultWindow(window.day().name(),
                window.start().toString(), window.end().toString()));
        }
        properties.setDefaultWindows(windows);
        return properties;
    }

    List<DemoWindow> defaultAgenda() {
        List<DemoWindow> agenda = new ArrayList<>();
        for (var day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            agenda.add(new DemoWindow(day, LocalTime.of(9, 0), LocalTime.of(13, 0)));
            agenda.add(new DemoWindow(day, LocalTime.of(14, 0), LocalTime.of(19, 0)));
        }
        return agenda;
    }

    DemoSchedulePlan plan() {
        return new DemoSchedulePlanner().plan(NOW, ZONE, defaultAgenda());
    }
}
