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
    void expiredConfirmedTokenReturns410BeforeStatusChange() throws Exception {
        String plain = "B".repeat(43);
        when(tokenService.resolve(plain)).thenThrow(new PublicTokenExpiredException());

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
            .andExpect(status().isGone());
    }
}
