package com.turnosodontologia.waiting;

import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WaitingListRegistrationServiceTest {

    @Autowired
    WaitingListRegistrationService registrationService;

    @Autowired
    PatientRepository patients;

    @Autowired
    WaitingListEntryRepository waitingEntries;

    @Test
    void createsPatientAndWaitingEntryWhenDniIsNew() {
        WaitingListEntry created = registrationService.register(
            "27.123.456", "Paciente", "Ficticio", "paciente.ficticio@example.com",
            LocalDate.of(2030, 12, 10), LocalDate.of(2030, 12, 20));

        assertThat(patients.count()).isEqualTo(1);
        assertThat(waitingEntries.count()).isEqualTo(1);
        assertThat(created.getPatient().getDni()).isEqualTo("27123456");
        assertThat(created.getStatus()).isEqualTo(WaitingListStatus.WAITING);
    }

    @Test
    void reusesExistingPatientWithoutOverwritingProfile() {
        Patient existing = patients.saveAndFlush(new Patient(
            "27123456", "Paciente Original", "Apellido Original", "original@example.com"));

        WaitingListEntry created = registrationService.register(
            "27 123 456", "Nombre Enviado", "Apellido Enviado", "otro@example.com", null, null);

        Patient persisted = patients.findById(existing.getId()).orElseThrow();
        assertThat(created.getPatient().getId()).isEqualTo(existing.getId());
        assertThat(persisted.getFirstName()).isEqualTo("Paciente Original");
        assertThat(persisted.getLastName()).isEqualTo("Apellido Original");
        assertThat(persisted.getEmail()).isEqualTo("original@example.com");
        assertThat(patients.count()).isEqualTo(1);
        assertThat(waitingEntries.count()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidDniNameEmailAndDateRangeWithoutPersistence() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registrationService.register(
                "27123A56", "Paciente", "Ficticio", "paciente@example.com", null, null))
            .isInstanceOf(InvalidWaitingListDataException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registrationService.register(
                "27123456", "   ", "Ficticio", "paciente@example.com", null, null))
            .isInstanceOf(InvalidWaitingListDataException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registrationService.register(
                "27123456", "Paciente", "Ficticio", "paciente@ejemplo", null, null))
            .isInstanceOf(InvalidWaitingListDataException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registrationService.register(
                "27123456", "Paciente", "Ficticio", "paciente@example.com",
                LocalDate.of(2030, 12, 21), LocalDate.of(2030, 12, 20)))
            .isInstanceOf(InvalidWaitingListDataException.class);

        assertThat(patients.count()).isZero();
        assertThat(waitingEntries.count()).isZero();
    }

    @Test
    void rejectsInvalidDataForExistingDniWithoutChangingProfile() {
        Patient existing = patients.saveAndFlush(new Patient(
            "27123456", "Paciente Original", "Apellido Original", "original@example.com"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registrationService.register(
                "27123456", "Nombre Enviado", "Apellido Enviado", "invalido@ejemplo", null, null))
            .isInstanceOf(InvalidWaitingListDataException.class);

        Patient persisted = patients.findById(existing.getId()).orElseThrow();
        assertThat(persisted.getFirstName()).isEqualTo("Paciente Original");
        assertThat(persisted.getLastName()).isEqualTo("Apellido Original");
        assertThat(persisted.getEmail()).isEqualTo("original@example.com");
        assertThat(waitingEntries.count()).isZero();
    }
}
