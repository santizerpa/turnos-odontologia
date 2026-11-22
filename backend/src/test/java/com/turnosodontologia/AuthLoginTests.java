package com.turnosodontologia;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLoginTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DentistRepository dentists;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void loginWithValidCredentialsReturnsProfileWithoutPasswordHash() throws Exception {
        String email = AuthFixtures.uniqueEmail("login.ok");
        AuthFixtures.createActive(dentists, passwordEncoder, email, "demo-ficticia-123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"demo-ficticia-123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.firstName").value("Demo"))
            .andExpect(jsonPath("$.lastName").value("Odontologo"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password_hash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginWithWrongPasswordReturnsGenericMessage() throws Exception {
        String email = AuthFixtures.uniqueEmail("login.wrong");
        AuthFixtures.createActive(dentists, passwordEncoder, email, "demo-ficticia-123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"otra-clave\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    void loginWithUnknownEmailReturnsTheSameGenericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nadie@odontologia.invalid\",\"password\":\"demo-ficticia-123\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    void loginNormalizesUppercaseAndPaddedEmailWithoutModifyingTheDentist() throws Exception {
        String email = AuthFixtures.uniqueEmail("login.normaliza");
        AuthFixtures.createActive(dentists, passwordEncoder, email, "demo-ficticia-123");
        var updatedAtBeforeLogin = dentists.findByEmail(email).orElseThrow().getUpdatedAt();
        String typedEmail = "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + typedEmail + "\",\"password\":\"demo-ficticia-123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));

        var reloaded = dentists.findByEmail(email).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo(email);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedAtBeforeLogin);
    }
}
