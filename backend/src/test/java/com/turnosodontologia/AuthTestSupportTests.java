package com.turnosodontologia;

import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.support.AuthFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthTestSupportTests {

    @Autowired
    DentistRepository dentists;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void contextProvidesEncoderAndPersistsFictitiousDentist() {
        String email = AuthFixtures.uniqueEmail("soporte.activo");

        var dentist = AuthFixtures.createActive(dentists, passwordEncoder, email, "demo-ficticia-123");

        assertThat(dentist.getId()).isNotNull();
        assertThat(dentists.findByEmail(email)).isPresent();
        assertThat(passwordEncoder.matches("demo-ficticia-123", dentist.getPasswordHash())).isTrue();
    }
}
