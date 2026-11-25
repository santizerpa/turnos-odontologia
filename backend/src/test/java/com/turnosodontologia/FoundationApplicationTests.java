package com.turnosodontologia;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.core.env.StandardEnvironment;
import org.junit.jupiter.api.Assertions;

import com.turnosodontologia.common.LocalProfileEnvironmentValidator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FoundationApplicationTests {

    private static final String FRONTEND_FALLBACK_COPY =
        "Frontend sin compilar. Ejecutá npm run build en la carpeta frontend y recargá.";

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoadsWithTestProfile() {
    }

    @Test
    void healthIsPublicAndExact() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}", true));
    }

    @Test
    void publicStaticResourcesAreAllowed() throws Exception {
        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(result -> assertThat(new String(
                result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
                .contains(FRONTEND_FALLBACK_COPY));
        mockMvc.perform(get("/admin.html"))
            .andExpect(status().isOk())
            .andExpect(result -> assertThat(new String(
                result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
                .contains(FRONTEND_FALLBACK_COPY));
        mockMvc.perform(get("/js/main.js")).andExpect(status().isOk());
    }

    @Test
    void protectedRouteReturnsUnauthorizedWithoutSession() throws Exception {
        mockMvc.perform(get("/api/appointments")).andExpect(status().isUnauthorized());
    }

    @Test
    void localProfileReportsAllMissingVariables() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("local");
        ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(
            new DefaultBootstrapContext(), new SpringApplication(TurnosOdontologiaApplication.class),
            new String[0], environment);

        IllegalStateException exception = Assertions.assertThrows(
            IllegalStateException.class,
            () -> new LocalProfileEnvironmentValidator().onApplicationEvent(event));

        Assertions.assertTrue(exception.getMessage().contains("SPRING_DATASOURCE_URL"));
        Assertions.assertTrue(exception.getMessage().contains("SPRING_DATASOURCE_USERNAME"));
        Assertions.assertTrue(exception.getMessage().contains("SPRING_DATASOURCE_PASSWORD"));
    }
}
