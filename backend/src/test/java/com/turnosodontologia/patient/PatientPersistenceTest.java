package com.turnosodontologia.patient;

import jakarta.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class PatientPersistenceTest {

    @Autowired
    PatientRepository patients;

    @Test
    void findsOneRowByNormalizedDniFromFormattedInput() {
        patients.saveAndFlush(new Patient("27.123.456", "Paciente", "Ficticio", "paciente.ficticio@example.com"));

        Patient found = patients.findByDni("27123456").orElseThrow();

        assertThat(found.getDni()).isEqualTo("27123456");
        assertThat(found.getEmail()).isEqualTo("paciente.ficticio@example.com");
        assertThat(patients.count()).isEqualTo(1);
    }

    @Test
    void rejectsSecondPatientWithEquivalentDni() {
        patients.saveAndFlush(new Patient("27.123.456", "Paciente", "Ficticio", "paciente.ficticio@example.com"));
        patients.save(new Patient("27123456", "Otro", "Ficticio", "otro.ficticio@example.com"));

        assertThatThrownBy(() -> patients.flush()).isInstanceOfAny(
            DataIntegrityViolationException.class,
            PersistenceException.class,
            ConstraintViolationException.class);
    }
}
