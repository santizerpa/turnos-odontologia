package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.*;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class C10ReminderReissueEndToEndTests {
    @Autowired MockMvc mockMvc;
    @Autowired Clock scheduleClock;
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentTokenRepository tokens;
    @Autowired AppointmentTokenService tokenService;
    @Autowired ReminderGenerationService reminders;
    @Autowired OutboxMessageRepository outbox;

    private Dentist dentist;
    private Patient patient;
    private Appointment appointment;

    @AfterEach
    void cleanCreatedData() {
        if (appointment != null) {
            outbox.findAll().stream()
                .filter(message -> appointment.getId().equals(message.getAppointment().getId()))
                .forEach(message -> outbox.deleteById(message.getId()));
            tokens.findByAppointmentId(appointment.getId()).ifPresent(token -> tokens.deleteById(token.getId()));
            appointments.deleteById(appointment.getId());
        }
        if (patient != null) patients.deleteById(patient.getId());
        if (dentist != null) dentists.deleteById(dentist.getId());
    }

    @Test
    void oldReservationTokenStopsResolvingAndReminderTokenResolvesPublicly() throws Exception {
        dentist = dentists.save(new Dentist("reissue.e2e@demo.invalid", "hash-ficticio", "Ana", "Demo"));
        patient = patients.save(new Patient("81003001", "Paciente", "Reemision", "reissue.patient@demo.invalid"));
        appointment = appointments.save(Appointment.create(
            dentist, patient, scheduleClock.instant().plusSeconds(36 * 3600), 30, CreatedBy.DENTIST));

        String oldToken = tokenService.issue(appointment);

        ReminderGenerationResult result = reminders.generate();

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        OutboxMessage reminder = outbox.findAll().stream()
            .filter(message -> appointment.getId().equals(message.getAppointment().getId()))
            .filter(message -> message.getType() == OutboxMessageType.REMINDER)
            .findFirst().orElseThrow();
        Matcher matcher = Pattern.compile("/api/public/appointments/([A-Za-z0-9_-]{43})/confirm")
            .matcher(reminder.getBody());
        assertThat(matcher.find()).isTrue();
        String newToken = matcher.group(1);

        mockMvc.perform(get("/api/public/appointments/{token}/confirm", oldToken))
            .andExpect(mvcResult -> assertThat(mvcResult.getResponse().getStatus()).isIn(400, 404, 409));
        mockMvc.perform(get("/api/public/appointments/{token}/confirm", newToken))
            .andExpect(status().isOk());
    }
}
