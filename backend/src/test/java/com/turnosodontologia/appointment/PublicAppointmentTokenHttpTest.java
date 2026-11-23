package com.turnosodontologia.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicAppointmentTokenHttpTest {
    @Autowired MockMvc mockMvc;
    @MockBean AppointmentTokenService tokenService;
    @MockBean AppointmentService appointmentService;

    @Test
    void missingTokenReturns404WithoutAppointmentInteraction() throws Exception {
        String plain = "A".repeat(43);
        when(tokenService.resolve(plain)).thenThrow(new PublicTokenInvalidException());

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
            .andExpect(status().isNotFound());
        verify(tokenService).resolve(plain);
    }

    @Test
    void publicHandlerMapsIllegalStateTo409WithoutInternalText() throws Exception {
        String plain = "C".repeat(43);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        when(record.getAppointment()).thenReturn(appointment);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.PENDING);
        when(appointment.getId()).thenReturn(java.util.UUID.fromString("00000000-0000-0000-0000-000000000303"));
        when(tokenService.resolve(plain)).thenReturn(record);
        when(appointmentService.changeStatus(eq(appointment.getId()), eq(AppointmentStatus.CONFIRMED), eq(Actor.PATIENT), org.mockito.ArgumentMatchers.anyString()))
            .thenThrow(new IllegalStateException("detalle interno prohibido"));

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("No se pudo confirmar el turno"))
            .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString()).doesNotContain("detalle interno prohibido"));
    }

    @Test
    void getConfirmAndCancelReturnMinimumAppointmentInfoWithoutDomainCalls() throws Exception {
        String plain = "G".repeat(43);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        java.util.UUID appointmentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000307");
        when(record.getAppointment()).thenReturn(appointment);
        when(appointment.getId()).thenReturn(appointmentId);
        when(appointment.getStartAt()).thenReturn(java.time.Instant.parse("2030-01-03T10:00:00Z"));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.PENDING);
        when(tokenService.resolve(plain)).thenReturn(record);

        mockMvc.perform(get("/api/public/appointments/{token}/confirm", plain))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startAt").value("2030-01-03T10:00:00Z"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.patient").doesNotExist());
        mockMvc.perform(get("/api/public/appointments/{token}/cancel", plain))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startAt").value("2030-01-03T10:00:00Z"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.patient").doesNotExist());

        verify(appointmentService, never()).changeStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(appointmentService, never()).cancel(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getConfirmAndCancelMapMissingAndExpiredTokens() throws Exception {
        String missing = "A".repeat(43);
        String expired = "B".repeat(43);
        when(tokenService.resolve(missing)).thenThrow(new PublicTokenInvalidException());
        when(tokenService.resolve(expired)).thenThrow(new PublicTokenExpiredException());

        mockMvc.perform(get("/api/public/appointments/{token}/confirm", missing))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Token inexistente o inválido"));
        mockMvc.perform(get("/api/public/appointments/{token}/cancel", expired))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.error").value("Token vencido"));
    }

    @Test
    void pendingTokenDelegatesToDomainChangeStatusOnce() throws Exception {
        String plain = "P".repeat(43);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        java.util.UUID appointmentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000301");
        when(record.getAppointment()).thenReturn(appointment);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.PENDING);
        when(appointment.getId()).thenReturn(appointmentId);
        when(tokenService.resolve(plain)).thenReturn(record);
        when(appointmentService.changeStatus(eq(appointmentId), eq(AppointmentStatus.CONFIRMED), eq(Actor.PATIENT), org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(appointment);

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("confirmación exitosa"));
        verify(appointmentService).changeStatus(eq(appointmentId), eq(AppointmentStatus.CONFIRMED), eq(Actor.PATIENT), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void confirmedTokenReturns200AndDoesNotCallDomainService() throws Exception {
        String plain = "D".repeat(43);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        when(record.getAppointment()).thenReturn(appointment);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);
        when(tokenService.resolve(plain)).thenReturn(record);

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("ya confirmado"));
        verify(appointmentService, never()).changeStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancelledTokenReturns409AndDoesNotCallDomainService() throws Exception {
        String plain = "E".repeat(43);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        when(record.getAppointment()).thenReturn(appointment);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CANCELLED);
        when(tokenService.resolve(plain)).thenReturn(record);

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("El turno no puede confirmarse"));
        verify(appointmentService, never()).changeStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void absentAndAttendedTokensAreRejectedWithoutDomainService() throws Exception {
        for (AppointmentStatus terminalStatus : new AppointmentStatus[]{AppointmentStatus.ABSENT, AppointmentStatus.ATTENDED}) {
            String plain = (terminalStatus == AppointmentStatus.ABSENT ? "F" : "G") + "A".repeat(42);
            Appointment appointment = mock(Appointment.class);
            AppointmentToken record = mock(AppointmentToken.class);
            when(record.getAppointment()).thenReturn(appointment);
            when(appointment.getStatus()).thenReturn(terminalStatus);
            when(tokenService.resolve(plain)).thenReturn(record);

            mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El turno no puede confirmarse"));
        }
        verify(appointmentService, never()).changeStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void confirmedTokenCancelsThroughDomainServiceOnce() throws Exception {
        String plain = "H".repeat(43);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        java.util.UUID appointmentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000304");
        when(record.getAppointment()).thenReturn(appointment);
        when(appointment.getId()).thenReturn(appointmentId);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);
        when(tokenService.resolve(plain)).thenReturn(record);
        when(appointmentService.cancel(eq(appointmentId), eq(Actor.PATIENT), org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(CancelAppointmentResult.completed());

        mockMvc.perform(post("/api/public/appointments/{token}/cancel", plain).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("cancelación exitosa"));
        verify(appointmentService).cancel(eq(appointmentId), eq(Actor.PATIENT), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void cancellationWithin24HoursReturnsSafeConflict() throws Exception {
        String plain = "I".repeat(43);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        java.util.UUID appointmentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000305");
        when(record.getAppointment()).thenReturn(appointment);
        when(appointment.getId()).thenReturn(appointmentId);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);
        when(tokenService.resolve(plain)).thenReturn(record);
        when(appointmentService.cancel(eq(appointmentId), eq(Actor.PATIENT), org.mockito.ArgumentMatchers.anyString()))
            .thenThrow(new IllegalStateException("detalle interno de 24 horas"));

        mockMvc.perform(post("/api/public/appointments/{token}/cancel", plain).with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Debe contactarse al consultorio para cancelar el turno"))
            .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                .doesNotContain("detalle interno"));
    }

    @Test
    void repeatedCancellationDelegatesAndReturnsAlreadyCancelled() throws Exception {
        String plain = "J".repeat(43);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        java.util.UUID appointmentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000306");
        when(record.getAppointment()).thenReturn(appointment);
        when(appointment.getId()).thenReturn(appointmentId);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CANCELLED);
        when(tokenService.resolve(plain)).thenReturn(record);
        when(appointmentService.cancel(eq(appointmentId), eq(Actor.PATIENT), org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(CancelAppointmentResult.alreadyCancelled());

        mockMvc.perform(post("/api/public/appointments/{token}/cancel", plain).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("ya cancelado"));
        verify(appointmentService).cancel(eq(appointmentId), eq(Actor.PATIENT), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void expiredConfirmedTokenReturns410BeforeStatusChange() throws Exception {
        String plain = "B".repeat(43);
        when(tokenService.resolve(plain)).thenThrow(new PublicTokenExpiredException());

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
            .andExpect(status().isGone());
    }
}
