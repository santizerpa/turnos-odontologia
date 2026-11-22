package com.turnosodontologia.patient;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PatientRepository patients;

    @Test
    void createsReusesUpdatesAndFiltersPatientsWithFictitiousData() throws Exception {
        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dni":"27123456","nombre":"Paciente","apellido":"Ficticio","email":"  Paciente.Ficticio@Example.COM  "}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("paciente.ficticio@example.com"));

        Patient created = patients.findByDni("27123456").orElseThrow();
        UUID id = created.getId();
        assertThat(created.getEmail()).isEqualTo("paciente.ficticio@example.com");

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dni":"27.123.456","nombre":"Paciente Actualizado","apellido":"Ficticio","email":"Nuevo.Ficticio@Consultorio.Example.com"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("nuevo.ficticio@consultorio.example.com"));

        Patient updated = patients.findByDni("27123456").orElseThrow();

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dni":"27123456","nombre":"Cambio","apellido":"Cambio","email":"paciente.ficticio@ejemplo"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Datos de paciente inválidos."));

        Patient unchanged = patients.findByDni("27123456").orElseThrow();

        mockMvc.perform(get("/api/patients")
                .param("dni", "27.123.456")
                .param("page", "0")
                .param("size", "2")
                .with(user("dentista")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].dni").value("27123456"))
            .andExpect(jsonPath("$.content[0].nombre").value("Paciente Actualizado"));

        assertAll(
            () -> assertThat(updated.getId()).isEqualTo(id),
            () -> assertThat(updated.getFirstName()).isEqualTo("Paciente Actualizado"),
            () -> assertThat(unchanged.getId()).isEqualTo(id),
            () -> assertThat(unchanged.getFirstName()).isEqualTo("Paciente Actualizado"),
            () -> assertThat(unchanged.getEmail()).isEqualTo("nuevo.ficticio@consultorio.example.com"),
            () -> assertThat(patients.count()).isEqualTo(1)
        );
    }
}
