package com.turnosodontologia;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.support.AuthFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIdentityRejectionTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DentistRepository dentists;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void deletedDentistCannotLoginAndKeepsItsDeletedAt() throws Exception {
        String email = AuthFixtures.uniqueEmail("login.baja");
        Dentist deleted = AuthFixtures.createDeleted(dentists, passwordEncoder, email, "demo-ficticia-123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"demo-ficticia-123\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Credenciales inválidas"))
            .andExpect(cookie().doesNotExist("JSESSIONID"));

        Dentist reloaded = dentists.findByEmail(email).orElseThrow();
        assertThat(reloaded.isDeleted()).isTrue();
        assertThat(reloaded.getDeletedAt()).isEqualTo(deleted.getDeletedAt());
    }

    @Test
    void plaintextPasswordHashNeverAuthenticates() throws Exception {
        String email = AuthFixtures.uniqueEmail("login.plano");
        dentists.save(new Dentist(email, "texto-plano", "Demo", "Odontologo"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"texto-plano\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Credenciales inválidas"))
            .andExpect(cookie().doesNotExist("JSESSIONID"));
    }
}
