package com.turnosodontologia.appointment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Service
public class AppointmentTokenService {
    private final AppointmentTokenRepository tokens;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AppointmentTokenService(AppointmentTokenRepository tokens, Clock scheduleClock) {
        this.tokens = tokens;
        this.clock = scheduleClock;
    }

    @Transactional
    public String issue(Appointment appointment) {
        if (appointment == null) throw new IllegalArgumentException("Turno requerido");

        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String hash = AppointmentToken.sha256(plainToken);

        Instant now = clock.instant();
        Instant expiresAt = appointment.getStartAt().isBefore(now.plusSeconds(7 * 24 * 60 * 60))
            ? appointment.getStartAt()
            : now.plusSeconds(7 * 24 * 60 * 60);

        AppointmentToken existing = tokens.findByAppointmentId(appointment.getId()).orElse(null);
        if (existing != null) {
            existing.replaceOpaqueTokenHashAndExpiry(hash, expiresAt);
            tokens.saveAndFlush(existing);
        } else {
            tokens.saveAndFlush(new AppointmentToken(appointment, hash, expiresAt));
        }
        return plainToken;
    }
}
