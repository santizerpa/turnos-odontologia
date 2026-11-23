package com.turnosodontologia.appointment;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppointmentApiTests {
    @Autowired MockMvc mockMvc;
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired PasswordEncoder passwordEncoder;

    private Dentist dentist;

    @BeforeEach
    void setup() {
        dentist = AuthFixtures.createActive(dentists, passwordEncoder,
            AuthFixtures.uniqueEmail("appointments.http"), "demo-ficticia-123");
    }

    @Test
    void getProtectedRangeOrderedAndInvalidAndUnauthorized() throws Exception {
        Patient patient = patients.save(new Patient("22345671", "Agenda", "Ficticia", "api.agenda@odontologia.invalid"));
        Appointment later = Appointment.create(dentist, patient, Instant.parse("2030-01-02T11:30:00Z"), 30, CreatedBy.DENTIST);
        Appointment earlier = Appointment.create(dentist, patient, Instant.parse("2030-01-02T10:00:00Z"), 30, CreatedBy.DENTIST);
        appointments.save(later);
        appointments.save(earlier);
        MockHttpSession session = login(dentist.getEmail(), "demo-ficticia-123");

        mockMvc.perform(get("/api/appointments").session(session)
                .param("from", "2030-01-02T09:00:00Z").param("to", "2030-01-02T12:00:00Z"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].startAt").value("2030-01-02T10:00:00Z"));

        mockMvc.perform(get("/api/appointments").session(session)
                .param("from", "2030-01-02T12:00:00Z").param("to", "2030-01-02T09:00:00Z"))
            .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/appointments")
                .param("from", "2030-01-02T09:00:00Z").param("to", "2030-01-02T12:00:00Z"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void patchCancelRepeatInvalidNotFoundAndUnauthorized() throws Exception {
        Patient patient = patients.save(new Patient("32345672", "Estado", "Ficticio", "api.estado@odontologia.invalid"));
        Appointment appointment = appointments.save(Appointment.create(dentist, patient,
            Instant.parse("2030-01-03T10:00:00Z"), 30, CreatedBy.DENTIST));
        MockHttpSession session = login(dentist.getEmail(), "demo-ficticia-123");

        mockMvc.perform(patch("/api/appointments/{id}/status", appointment.getId()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\",\"reason\":\"Ficticio\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("cancelado"));
        mockMvc.perform(patch("/api/appointments/{id}/status", appointment.getId()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\",\"reason\":\"Ficticio\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("ya cancelado"));

        mockMvc.perform(patch("/api/appointments/{id}/status", appointment.getId()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\",\"reason\":\"volver\"}"))
            .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(patch("/api/appointments/{id}/status", "00000000-0000-0000-0000-000000000999").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\",\"reason\":\"n/a\"}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/appointments/{id}/status", appointment.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\",\"reason\":\"n/a\"}"))
            .andExpect(status().isUnauthorized());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
