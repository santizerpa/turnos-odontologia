package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import com.turnosodontologia.support.AuthFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.demo.inbox.enabled=true")
@Transactional
class PatientInboxHttpTests {
    @Autowired MockMvc mockMvc;
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired OutboxMessageRepository outbox;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void fictitiousMessages() {
        Dentist dentist = AuthFixtures.createActive(dentists, passwordEncoder,
            AuthFixtures.uniqueEmail("inbox.http"), "demo-ficticia-123");
        Patient patient = patients.save(new Patient("42000001", "Bandeja", "Ficticia", "inbox.paciente@demo.invalid"));
        Patient other = patients.save(new Patient("42000002", "Otro", "Ficticio", "inbox.otro@demo.invalid"));
        Appointment first = appointments.save(Appointment.create(dentist, patient,
            Instant.parse("2030-02-01T13:00:00Z"), 30, CreatedBy.PATIENT));
        Appointment second = appointments.save(Appointment.create(dentist, patient,
            Instant.parse("2030-02-02T17:30:00Z"), 30, CreatedBy.PATIENT));
        Appointment foreign = appointments.save(Appointment.create(dentist, other,
            Instant.parse("2030-02-03T13:00:00Z"), 30, CreatedBy.PATIENT));

        save(first, "inbox.paciente@demo.invalid", "Confirma tu turno", "2030-01-20T10:00:00Z");
        save(second, "inbox.paciente@demo.invalid", "Recordatorio de turno", "2030-01-21T10:00:00Z");
        save(null, "inbox.paciente@demo.invalid", "Aviso sin turno", "2030-01-19T10:00:00Z");
        save(foreign, "inbox.otro@demo.invalid", "Confirma tu turno", "2030-01-22T10:00:00Z");
    }

    private void save(Appointment appointment, String email, String subject, String createdAt) {
        OutboxMessage message = new OutboxMessage(appointment, email, subject,
            "Para confirmar tu turno, visita /api/public/appointments/token-ficticio/confirm.", OutboxStatus.PENDING);
        ReflectionTestUtils.setField(message, "createdAt", Instant.parse(createdAt));
        outbox.save(message);
    }

    @Test
    void listsOnlyMessagesForNormalizedEmailNewestFirstWithoutSession() throws Exception {
        var result = mockMvc.perform(get("/api/public/inbox").param("email", "  Inbox.Paciente@Demo.INVALID "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].subject").value("Recordatorio de turno"))
            .andExpect(jsonPath("$[0].recipientEmail").value("inbox.paciente@demo.invalid"))
            .andExpect(jsonPath("$[0].patientDni").value("42000001"))
            .andExpect(jsonPath("$[0].appointmentStartAt").value("2030-02-02T17:30:00Z"))
            .andExpect(jsonPath("$[0].appointmentDurationMinutes").value(30))
            .andExpect(jsonPath("$[0].createdAt").value("2030-01-21T10:00:00Z"))
            .andExpect(jsonPath("$[0].body").exists())
            .andExpect(jsonPath("$[1].subject").value("Confirma tu turno"))
            .andExpect(jsonPath("$[2].subject").value("Aviso sin turno"))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("42000002", "inbox.otro@demo.invalid", "tokenHash", "status", "sentAt");
    }

    @Test
    void messageWithoutAppointmentHasNullAppointmentFields() throws Exception {
        mockMvc.perform(get("/api/public/inbox").param("email", "inbox.paciente@demo.invalid"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[2].patientDni").value(nullValue()))
            .andExpect(jsonPath("$[2].appointmentStartAt").value(nullValue()))
            .andExpect(jsonPath("$[2].appointmentDurationMinutes").value(nullValue()));
    }

    @Test
    void invalidOrMissingEmailIs422() throws Exception {
        for (String email : new String[] {"", "   ", "no-es-un-email"}) {
            mockMvc.perform(get("/api/public/inbox").param("email", email))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Email inválido"));
        }
        mockMvc.perform(get("/api/public/inbox"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("Email inválido"));
    }

    @Test
    void emailWithoutMessagesReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/public/inbox").param("email", "nadie@demo.invalid"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
