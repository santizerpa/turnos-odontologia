package com.turnosodontologia.waiting;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WaitingListApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PatientRepository patients;

    @Autowired
    WaitingListEntryRepository waitingEntries;

    @Autowired
    DentistRepository dentists;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Dentist dentist;

    @BeforeEach
    void setUpDentist() {
        dentist = AuthFixtures.createActive(dentists, passwordEncoder,
            AuthFixtures.uniqueEmail("waiting-list.http"), "demo-ficticia-123");
    }

    @Test
    void publicPostCreatesWaitingEntryWithoutSessionOrPatientDataInResponse() throws Exception {
        mockMvc.perform(post("/api/public/waiting-list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dni":"90.123.456","nombre":"Paciente","apellido":"Ficticio",
                     "email":"paciente.ficticio@example.com","requestedDateFrom":"2030-12-10",
                     "requestedDateTo":"2030-12-20"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.requestedDateFrom").value("2030-12-10"))
            .andExpect(jsonPath("$.requestedDateTo").value("2030-12-20"))
            .andExpect(jsonPath("$.dni").doesNotExist())
            .andExpect(jsonPath("$.email").doesNotExist());

        Patient patient = patients.findByDni("90123456").orElseThrow();
        assertThat(waitingEntries.count()).isEqualTo(1);
        assertThat(patient.getEmail()).isEqualTo("paciente.ficticio@example.com");
    }

    @Test
    void authenticatedGetListsOldestFirstAndRejectsMissingSession() throws Exception {
        Patient oldestPatient = patients.saveAndFlush(new Patient(
            "90123457", "Paciente Antiguo", "Ficticio", "antiguo@example.com"));
        Patient newestPatient = patients.saveAndFlush(new Patient(
            "90123458", "Paciente Nuevo", "Ficticio", "nuevo@example.com"));
        WaitingListEntry oldest = waitingEntries.saveAndFlush(new WaitingListEntry(
            oldestPatient, null, null, WaitingListStatus.WAITING));
        WaitingListEntry newest = waitingEntries.saveAndFlush(new WaitingListEntry(
            newestPatient, null, null, WaitingListStatus.NOTIFIED));
        jdbcTemplate.update("UPDATE waiting_list_entries SET created_at = ? WHERE id = ?",
            Timestamp.from(Instant.parse("2030-01-01T10:00:00Z")), oldest.getId());
        jdbcTemplate.update("UPDATE waiting_list_entries SET created_at = ? WHERE id = ?",
            Timestamp.from(Instant.parse("2030-01-02T10:00:00Z")), newest.getId());

        mockMvc.perform(get("/api/waiting-list"))
            .andExpect(status().isUnauthorized());

        MockHttpSession session = login();
        mockMvc.perform(get("/api/waiting-list").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(oldest.getId().toString()))
            .andExpect(jsonPath("$[0].nombre").value("Paciente Antiguo"))
            .andExpect(jsonPath("$[0].status").value("WAITING"))
            .andExpect(jsonPath("$[1].id").value(newest.getId().toString()))
            .andExpect(jsonPath("$[1].status").value("NOTIFIED"))
            .andExpect(jsonPath("$[0].dni").doesNotExist());
    }

    @Test
    void publicPostReturnsSameCreatedStatusForExistingPatientAndKeepsSavedProfile() throws Exception {
        Patient existing = patients.saveAndFlush(new Patient(
            "90123456", "Perfil Original", "Apellido Original", "original@example.com"));

        mockMvc.perform(post("/api/public/waiting-list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dni":"90123456","nombre":"Nombre Enviado","apellido":"Apellido Enviado",
                     "email":"otro@example.com"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.dni").doesNotExist())
            .andExpect(jsonPath("$.email").doesNotExist());

        Patient persisted = patients.findById(existing.getId()).orElseThrow();
        assertThat(persisted.getFirstName()).isEqualTo("Perfil Original");
        assertThat(persisted.getLastName()).isEqualTo("Apellido Original");
        assertThat(persisted.getEmail()).isEqualTo("original@example.com");
    }

    @Test
    void invalidPublicPostUsesErrorBody() throws Exception {
        mockMvc.perform(post("/api/public/waiting-list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dni\":\"invalido\",\"nombre\":\"Paciente\",\"apellido\":\"Ficticio\",\"email\":\"x@example.invalid\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Datos de inscripción inválidos."));
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + dentist.getEmail() + "\",\"password\":\"demo-ficticia-123\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
