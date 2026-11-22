package com.turnosodontologia;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el contrato de expiración sin simular el paso de 30 minutos (design.md D9).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionExpiryTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ServerProperties serverProperties;

    @Test
    void sessionTimeoutIsConfiguredToThirtyMinutes() {
        Duration timeout = serverProperties.getServlet().getSession().getTimeout();

        assertThat(timeout).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void sessionCookieThatDoesNotExistReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("JSESSIONID", "sesion-inexistente")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("No autenticado"));
    }
}
