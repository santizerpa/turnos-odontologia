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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRouteSecurityTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DentistRepository dentists;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void protectedRoutesReturnJsonUnauthorizedWithoutSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("No autenticado"));
        mockMvc.perform(get("/api/appointments"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    void publicRoutesDoNotRequireSession() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/admin.html")).andExpect(status().isOk());
        mockMvc.perform(get("/js/app.js")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@odontologia.invalid\",\"password\":\"incorrecta\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/public/demo")).andExpect(status().isNotFound());
    }

    @Test
    void logoutWithSessionDoesNotRequireCsrfToken() throws Exception {
        String email = AuthFixtures.uniqueEmail("csrf.logout");
        AuthFixtures.createActive(dentists, passwordEncoder, email, "demo-ficticia-123");

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"demo-ficticia-123\"}"))
            .andExpect(status().isOk())
            .andReturn();

        mockMvc.perform(post("/api/auth/logout")
                .session((org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false)))
            .andExpect(status().isNoContent());
    }
}
