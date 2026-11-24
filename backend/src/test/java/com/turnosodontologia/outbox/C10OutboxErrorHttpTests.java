package com.turnosodontologia.outbox;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.support.AuthFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class C10OutboxErrorHttpTests {
    @Autowired MockMvc mockMvc;
    @Autowired DentistRepository dentists;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean ReminderGenerationService reminderGenerationService;

    private final List<UUID> createdDentistIds = new ArrayList<>();

    @AfterEach
    void cleanCreatedData() {
        createdDentistIds.forEach(id -> dentists.findById(id).ifPresent(dentists::delete));
    }

    @Test
    void unexpectedReminderGenerationFailureUsesFixedSafeResponse() throws Exception {
        Dentist dentist = AuthFixtures.createActive(dentists, passwordEncoder,
            AuthFixtures.uniqueEmail("c10.outbox.error"), "demo-ficticia-123");
        createdDentistIds.add(dentist.getId());
        MockHttpSession session = login(dentist.getEmail(), "demo-ficticia-123");

        when(reminderGenerationService.generate())
            .thenThrow(new RuntimeException("detalle-interno-secreto"));

        MvcResult result = mockMvc.perform(post("/api/outbox/reminders/run").session(session))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.title").value("Error interno"))
            .andExpect(jsonPath("$.detail").value("Error interno al procesar la bandeja"))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body)
            .doesNotContain("detalle-interno-secreto")
            .doesNotContain("Exception")
            .doesNotContain("at com.");
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
