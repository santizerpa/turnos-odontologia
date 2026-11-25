package com.turnosodontologia.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(AppointmentController.class)
@ActiveProfiles("test")
class AppointmentConflictHttpTest {
    @Autowired MockMvc mockMvc;

    @MockBean AppointmentService appointmentService;

    @Test
    void patchConflictMapsToHttp409() throws Exception {
        UUID appointmentId = UUID.fromString("00000000-0000-0000-0000-000000000409");
        when(appointmentService.changeStatus(eq(appointmentId), eq(AppointmentStatus.CONFIRMED),
            eq(Actor.DENTIST), any())).thenThrow(new AppointmentConflictException());

        mockMvc.perform(patch("/api/appointments/{id}/status", appointmentId)
                .with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\",\"reason\":\"franja ficticia\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("La franja solicitada ya está ocupada"));
    }

    @Test
    void unrelatedIntegrityViolationMapsToGeneric409WithoutDriverDetails() throws Exception {
        UUID appointmentId = UUID.fromString("00000000-0000-0000-0000-000000000410");
        String token = "integrity-token-sentinel";
        String dni = "integrity-dni-sentinel";
        when(appointmentService.changeStatus(eq(appointmentId), eq(AppointmentStatus.CONFIRMED),
            eq(Actor.DENTIST), any())).thenThrow(new DataIntegrityViolationException(
                "SQL driver detail " + token + " " + dni + " org.hibernate at com.example.Driver"));

        var result = mockMvc.perform(patch("/api/appointments/{id}/status", appointmentId)
                .with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\",\"reason\":\"franja ficticia\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("No se pudo completar la operación"))
            .andReturn();

        assertSanitizedErrorBody(result.getResponse().getContentAsString(), token, dni);
    }

    @Test
    void unexpectedIllegalArgumentAndIllegalStateUseFixed422Body() throws Exception {
        String sentinel = "SQL org.hibernate Exception at com.fake.Token DNI-UNICO";
        UUID appointmentId = UUID.fromString("00000000-0000-0000-0000-000000000411");
        when(appointmentService.changeStatus(eq(appointmentId), eq(AppointmentStatus.CONFIRMED),
            eq(Actor.DENTIST), any()))
            .thenThrow(new IllegalArgumentException(sentinel))
            .thenThrow(new IllegalStateException(sentinel));
        var invalidArgument = mockMvc.perform(patch("/api/appointments/{id}/status", appointmentId)
                .with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\",\"reason\":\"franja ficticia\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("Solicitud inválida"))
            .andReturn();
        assertSanitizedErrorBody(invalidArgument.getResponse().getContentAsString(), sentinel);

        var invalidState = mockMvc.perform(patch("/api/appointments/{id}/status", appointmentId)
                .with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\",\"reason\":\"franja ficticia\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("Solicitud inválida"))
            .andReturn();
        assertSanitizedErrorBody(invalidState.getResponse().getContentAsString(), sentinel);
    }

    private void assertSanitizedErrorBody(String body, String... sentinels) {
        assertThat(body).doesNotContain("Exception", "org.springframework", "org.hibernate", "SQL", "at com.");
        assertThat(body).doesNotContain(sentinels);
    }
}
