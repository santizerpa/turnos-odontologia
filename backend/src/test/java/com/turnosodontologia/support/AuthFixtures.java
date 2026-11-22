package com.turnosodontologia.support;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos ficticios para las pruebas de autenticación.
 * Nunca usar datos reales: todos los emails terminan en odontologia.invalid.
 */
public final class AuthFixtures {

    private AuthFixtures() {
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@odontologia.invalid";
    }

    public static Dentist createActive(DentistRepository dentists, PasswordEncoder encoder, String email, String rawPassword) {
        return dentists.save(new Dentist(email, encoder.encode(rawPassword), "Demo", "Odontologo"));
    }

    public static Dentist createDeleted(DentistRepository dentists, PasswordEncoder encoder, String email, String rawPassword) {
        Dentist dentist = createActive(dentists, encoder, email, rawPassword);
        dentist.markDeleted(Instant.parse("2026-11-01T12:00:00Z"));
        return dentists.save(dentist);
    }
}
