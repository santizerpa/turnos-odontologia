package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.appointment.AppointmentTokenRepository;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import com.turnosodontologia.support.AuthFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class C10OutboxHttpTests {
    @Autowired MockMvc mockMvc;
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired OutboxMessageRepository outbox;
    @Autowired AppointmentTokenRepository appointmentTokens;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired Clock scheduleClock;

    private final List<UUID> createdDentistIds = new ArrayList<>();
    private final List<UUID> createdPatientIds = new ArrayList<>();
    private final List<UUID> createdAppointmentIds = new ArrayList<>();

    @AfterEach
    void cleanCreatedData() {
        outbox.findAll().stream()
            .filter(message -> message.getAppointment() != null
                && createdAppointmentIds.contains(message.getAppointment().getId()))
            .forEach(outbox::delete);
        createdAppointmentIds.forEach(id -> appointmentTokens.findAll().stream()
            .filter(token -> token.getAppointment().getId().equals(id))
            .forEach(appointmentTokens::delete));
        createdAppointmentIds.forEach(id -> appointments.findById(id).ifPresent(appointments::delete));
        createdPatientIds.forEach(id -> patients.findById(id).ifPresent(patients::delete));
        createdDentistIds.forEach(id -> dentists.findById(id).ifPresent(dentists::delete));
    }

    @Test
    void runDispatchAndGetWithRealSessionAndVerifyDatabase() throws Exception {
        Dentist dentist = AuthFixtures.createActive(dentists, passwordEncoder,
            AuthFixtures.uniqueEmail("c10.http"), "demo-ficticia-123");
        createdDentistIds.add(dentist.getId());
        String dni = String.valueOf(80000000L + (UUID.randomUUID().getLeastSignificantBits() & 0x7fffff));
        Patient patient = patients.save(new Patient(dni, "Paciente", "Ficticio", "c10.http@demo.invalid"));
        createdPatientIds.add(patient.getId());
        Appointment appointment = appointments.save(Appointment.create(dentist, patient,
            scheduleClock.instant().plusSeconds(36 * 3600), 30, CreatedBy.DENTIST));
        createdAppointmentIds.add(appointment.getId());

        MockHttpSession session = login(dentist.getEmail(), "demo-ficticia-123");

        mockMvc.perform(post("/api/outbox/reminders/run").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(1));
        mockMvc.perform(post("/api/outbox/dispatch").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dispatched").value(1));
        mockMvc.perform(post("/api/outbox/dispatch").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dispatched").value(0));

        mockMvc.perform(get("/api/outbox/messages").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].appointmentId").value(appointment.getId().toString()))
            .andExpect(jsonPath("$[0].recipientEmail").value("c10.http@demo.invalid"))
            .andExpect(jsonPath("$[0].subject").exists()).andExpect(jsonPath("$[0].body").exists())
            .andExpect(jsonPath("$[0].status").value("SIMULATED_SENT"))
            .andExpect(jsonPath("$[0].type").value("REMINDER"))
            .andExpect(jsonPath("$[0].createdAt").exists()).andExpect(jsonPath("$[0].sentAt").exists())
            .andExpect(jsonPath("$[0].dni").doesNotExist())
            .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
            .andExpect(jsonPath("$[0].token").doesNotExist());

        OutboxMessage stored = outbox.findAll().stream().filter(m -> m.getAppointment() != null
            && m.getAppointment().getId().equals(appointment.getId())).findFirst().orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.SIMULATED_SENT);
        assertThat(stored.getSentAt()).isNotNull();
    }

    @Test
    void dtoToleratesMessageWithoutAppointmentAndDispatchIsIdempotent() throws Exception {
        Dentist dentist = AuthFixtures.createActive(dentists, passwordEncoder,
            AuthFixtures.uniqueEmail("c10.null.appointment"), "demo-ficticia-123");
        createdDentistIds.add(dentist.getId());
        OutboxMessage pending = outbox.save(new OutboxMessage(null,
            "sin-turno@demo.invalid", "Ficticio sin turno", "Cuerpo ficticio",
            OutboxStatus.PENDING, OutboxMessageType.BOOKING));
        MockHttpSession session = login(dentist.getEmail(), "demo-ficticia-123");

        mockMvc.perform(post("/api/outbox/dispatch").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dispatched").value(1));
        Instant sentAt = outbox.findById(pending.getId()).orElseThrow().getSentAt();
        mockMvc.perform(post("/api/outbox/dispatch").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dispatched").value(0));

        mockMvc.perform(get("/api/outbox/messages").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.recipientEmail == 'sin-turno@demo.invalid')].appointmentId").value((Object) null));
        assertThat(outbox.findById(pending.getId()).orElseThrow().getSentAt()).isEqualTo(sentAt);
    }

    @Test
    void allOutboxEndpointsRejectRequestsWithoutSession() throws Exception {
        mockMvc.perform(get("/api/outbox/messages")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/outbox/reminders/run")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/outbox/dispatch")).andExpect(status().isUnauthorized());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
