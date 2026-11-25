package com.turnosodontologia.schedule;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.support.AuthFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ScheduleHttpSecurityTests.TestClock.class)
class ScheduleHttpSecurityTests {
    private static final Instant TEST_NOW = Instant.parse("2026-11-25T12:00:00Z");
    private static final Clock TEST_CLOCK = Clock.fixed(TEST_NOW, ZoneOffset.UTC);
    @Autowired MockMvc mockMvc;
    @Autowired DentistRepository dentists;
    @Autowired ScheduleConfigurationRepository configurations;
    @Autowired PasswordEncoder passwordEncoder;

    private Dentist dentist;

    @BeforeEach
    void setup() {
        dentist = AuthFixtures.createActive(dentists, passwordEncoder,
                AuthFixtures.uniqueEmail("schedule.http"), "demo-ficticia-123");
    }

    @Test
    void unauthorizedRoutesPreserveAuthJson() throws Exception {
        mockMvc.perform(get("/api/schedule")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("No autenticado"));
        mockMvc.perform(patch("/api/schedule").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/schedule/availability")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/schedule/blocks").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/schedule/blocks/00000000-0000-0000-0000-000000000000")).andExpect(status().isUnauthorized());
    }

    @Test
    void getPatchAvailabilityAndBlocksUseAuthenticatedDentist() throws Exception {
        MockHttpSession session = login(dentist.getEmail(), "demo-ficticia-123");

        mockMvc.perform(get("/api/schedule").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Argentina/Buenos_Aires"))
                .andExpect(jsonPath("$.slotDurationMinutes").value(30))
                .andExpect(jsonPath("$.publicWindow.minNoticeHours").value(24))
                .andExpect(jsonPath("$.publicWindow.maxDays").value(30))
                .andExpect(jsonPath("$.days").isArray());

        String valid = "{\"timezone\":null,\"days\":[{\"day\":\"MONDAY\",\"windows\":[{\"start\":\"10:00\",\"end\":\"12:00\"}]},{\"day\":\"TUESDAY\",\"windows\":[]},{\"day\":\"WEDNESDAY\",\"windows\":[]},{\"day\":\"THURSDAY\",\"windows\":[]},{\"day\":\"FRIDAY\",\"windows\":[]}]}";
        mockMvc.perform(patch("/api/schedule").session(session).contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].windows[0].start").value("10:00"));

        mockMvc.perform(patch("/api/schedule").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"Europe/Madrid\",\"days\":[]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.details").isArray());

        mockMvc.perform(post("/api/schedule/blocks").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startAt\":\"2027-01-10T10:00:00-03:00\",\"endAt\":\"2027-01-10T11:00:00-03:00\",\"reason\":\"Ficticio\",\"dentistId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.reason").value("Ficticio"));

        mockMvc.perform(post("/api/schedule/blocks").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startAt\":\"2027-01-10T11:00:00-03:00\",\"endAt\":\"2027-01-10T10:00:00-03:00\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void authenticatedGetCreatesMissingScheduleConfiguration() throws Exception {
        MockHttpSession session = login(dentist.getEmail(), "demo-ficticia-123");
        long scheduleCountBefore = configurations.count();
        assertTrue(configurations.findByDentistId(dentist.getId()).isEmpty());

        mockMvc.perform(get("/api/schedule").session(session))
                .andExpect(status().isOk());

        assertTrue(configurations.findByDentistId(dentist.getId()).isPresent());
        assertEquals(scheduleCountBefore + 1, configurations.count());
    }

    @Test
    void availabilityAndAdviceForBadRanges() throws Exception {
        MockHttpSession session = login(dentist.getEmail(), "demo-ficticia-123");
        mockMvc.perform(get("/api/schedule").session(session)).andExpect(status().isOk());
        Instant testNow = TEST_CLOCK.instant();
        String validFrom = testNow.plusSeconds(3 * 86400L).atOffset(ZoneOffset.ofHours(-3)).toString();
        String validTo = testNow.plusSeconds(4 * 86400L).atOffset(ZoneOffset.ofHours(-3)).toString();
        String beforeMinimumFrom = testNow.minusSeconds(86400L).atOffset(ZoneOffset.ofHours(-3)).toString();
        String beforeMinimumTo = testNow.plusSeconds(24 * 3600L).atOffset(ZoneOffset.ofHours(-3)).toString();
        String beyondMaximumTo = testNow.plusSeconds(31 * 86400L).atOffset(ZoneOffset.ofHours(-3)).toString();

        mockMvc.perform(get("/api/schedule/availability").session(session).param("from", validFrom).param("to", validTo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Argentina/Buenos_Aires"))
                .andExpect(jsonPath("$.durationMinutes").value(30))
                .andExpect(jsonPath("$.slots").isArray());

        mockMvc.perform(get("/api/schedule/availability").session(session).param("from", beforeMinimumFrom).param("to", beforeMinimumTo))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/schedule/availability").session(session).param("to", beyondMaximumTo))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/schedule/availability").session(session).param("from", "malformado").param("to", beyondMaximumTo))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @TestConfiguration
    static class TestClock {
        @Bean
        @org.springframework.context.annotation.Primary
        Clock fixedClock() {
            return TEST_CLOCK;
        }
    }
}
