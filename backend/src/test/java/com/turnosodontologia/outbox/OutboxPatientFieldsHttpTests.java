package com.turnosodontologia.outbox;

import com.jayway.jsonpath.JsonPath;
import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import com.turnosodontologia.support.AuthFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OutboxPatientFieldsHttpTests {
    @Autowired MockMvc mockMvc;
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired OutboxMessageRepository outbox;
    @Autowired PasswordEncoder passwordEncoder;

    private void save(Appointment appointment, String email, String subject, String createdAt) {
        OutboxMessage message = new OutboxMessage(appointment, email, subject, "Mensaje ficticio.", OutboxStatus.PENDING);
        ReflectionTestUtils.setField(message, "createdAt", Instant.parse(createdAt));
        outbox.save(message);
    }

    @Test
    void listsPatientNameAndDniNewestFirst() throws Exception {
        Dentist dentist = AuthFixtures.createActive(dentists, passwordEncoder,
            AuthFixtures.uniqueEmail("outbox.patient"), "demo-ficticia-123");
        Patient patient = patients.save(new Patient("43000001", "Lucia", "Ficticia", "outbox.lucia@demo.invalid"));
        Appointment appointment = appointments.save(Appointment.create(dentist, patient,
            Instant.parse("2031-03-01T13:00:00Z"), 30, CreatedBy.PATIENT));

        // Far-future send dates so these are the newest rows even with data left by other suites.
        save(appointment, "outbox.lucia@demo.invalid", "Mensaje viejo", "2031-01-01T10:00:00Z");
        save(appointment, "outbox.lucia@demo.invalid", "Mensaje nuevo", "2031-01-03T10:00:00Z");
        save(null, "outbox.sin-turno@demo.invalid", "Mensaje del medio", "2031-01-02T10:00:00Z");

        var result = mockMvc.perform(get("/api/outbox/messages").with(user("dentista")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].subject").value("Mensaje nuevo"))
            .andExpect(jsonPath("$[0].patientName").value("Lucia Ficticia"))
            .andExpect(jsonPath("$[0].patientDni").value("43000001"))
            .andExpect(jsonPath("$[1].subject").value("Mensaje del medio"))
            .andExpect(jsonPath("$[1].patientName").value(nullValue()))
            .andExpect(jsonPath("$[1].patientDni").value(nullValue()))
            .andExpect(jsonPath("$[2].subject").value("Mensaje viejo"))
            .andExpect(jsonPath("$[0].token").doesNotExist())
            .andReturn();

        List<String> createdAt = JsonPath.read(result.getResponse().getContentAsString(), "$[*].createdAt");
        assertThat(createdAt).isSortedAccordingTo((left, right) -> Instant.parse(right).compareTo(Instant.parse(left)));
    }
}
