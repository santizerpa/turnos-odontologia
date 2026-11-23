package com.turnosodontologia.appointment;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentTokenServiceValidationTest {
    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");

    @Test
    void hashAndLookupRejectInvalidOrMissingTokenBeforeAppointmentState() {
        AppointmentTokenRepository tokens = mock(AppointmentTokenRepository.class);
        AppointmentTokenService service = new AppointmentTokenService(tokens, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.resolve(null)).isInstanceOf(PublicTokenInvalidException.class);
        assertThatThrownBy(() -> service.resolve("corta")).isInstanceOf(PublicTokenInvalidException.class);

        String plain = "A".repeat(43);
        when(tokens.findByTokenHash(AppointmentToken.sha256(plain))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve(plain)).isInstanceOf(PublicTokenInvalidException.class);
    }

    @Test
    void expiredTokenWinsOverConfirmedAppointmentWithoutStatusAccess() {
        AppointmentTokenRepository tokens = mock(AppointmentTokenRepository.class);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        String plain = "C".repeat(43);

        when(record.getExpiresAt()).thenReturn(NOW);
        when(record.getAppointment()).thenReturn(appointment);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);
        when(tokens.findByTokenHash(AppointmentToken.sha256(plain))).thenReturn(Optional.of(record));

        AppointmentTokenService service = new AppointmentTokenService(tokens, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.resolve(plain)).isInstanceOf(PublicTokenExpiredException.class);
        org.mockito.Mockito.verify(appointment, org.mockito.Mockito.never()).getStatus();
    }

    @Test
    void exactlyExpiryIsExpiredBeforeCheckingAppointment() {
        AppointmentTokenRepository tokens = mock(AppointmentTokenRepository.class);
        Appointment appointment = mock(Appointment.class);
        AppointmentToken record = mock(AppointmentToken.class);
        String plain = "A".repeat(43);

        when(record.getExpiresAt()).thenReturn(NOW);
        when(record.getAppointment()).thenReturn(appointment);
        when(tokens.findByTokenHash(AppointmentToken.sha256(plain))).thenReturn(Optional.of(record));

        AppointmentTokenService service = new AppointmentTokenService(tokens, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.resolve(plain)).isInstanceOf(PublicTokenExpiredException.class);
    }
}
