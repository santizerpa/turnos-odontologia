package com.turnosodontologia.patient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PatientNameSearchIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired PatientRepository patients;

    @BeforeEach
    void fictitiousPatients() {
        // Distinctive fictitious names so other suites sharing the H2 database cannot match.
        patients.save(new Patient("41000001", "Qwana", "Ejemplo", "qwana.ejemplo@demo.invalid"));
        patients.save(new Patient("41000002", "Bruno", "Qwdemo", "bruno.qwdemo@demo.invalid"));
        patients.save(new Patient("41000003", "Maqwana", "Prueba", "maqwana.prueba@demo.invalid"));
    }

    @Test
    void partialCaseInsensitiveMatchOnFirstOrLastName() throws Exception {
        mockMvc.perform(get("/api/patients").param("nombre", "  QWANA ").param("page", "0").param("size", "10")
                .with(user("dentista")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[*].dni").value(containsInAnyOrder("41000001", "41000003")))
            .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/patients").param("nombre", "qwdemo").with(user("dentista")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].dni").value("41000002"));
    }

    @Test
    void matchesFirstAndLastNameTogether() throws Exception {
        mockMvc.perform(get("/api/patients").param("nombre", "bruno qwdemo").with(user("dentista")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].nombre").value("Bruno"));
    }

    @Test
    void noMatchesReturnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/patients").param("nombre", "zzqqxx").with(user("dentista")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void dniAndNameTogetherAreRejected() throws Exception {
        mockMvc.perform(get("/api/patients").param("dni", "41000001").param("nombre", "qwana").with(user("dentista")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Datos de paciente inválidos."));
    }

    @Test
    void blankNameBehavesAsNoFilter() throws Exception {
        mockMvc.perform(get("/api/patients").param("nombre", "   ").param("size", "100").with(user("dentista")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(patients.count()));
    }
}
