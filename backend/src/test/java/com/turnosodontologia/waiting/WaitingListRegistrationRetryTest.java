package com.turnosodontologia.waiting;

import com.turnosodontologia.patient.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WaitingListRegistrationRetryTest {

    @Test
    void retriesInNewTransactionAndReusesPatientAfterConcurrentDniInsert() {
        String dni = "27123456";
        String firstName = "Paciente";
        String lastName = "Ficticio";
        String email = "paciente.ficticio@example.com";
        LocalDate from = LocalDate.of(2030, 12, 10);
        LocalDate to = LocalDate.of(2030, 12, 20);
        Patient existing = new Patient(dni, "Perfil Original", "Apellido Original", "original@example.com");
        WaitingListEntry waitingEntry = new WaitingListEntry(existing, from, to, WaitingListStatus.WAITING);
        AtomicInteger createCalls = new AtomicInteger();
        AtomicInteger retryCalls = new AtomicInteger();

        WaitingListRegistrationTransaction transactions = new WaitingListRegistrationTransaction(null, null) {
            @Override
            public WaitingListEntry create(String normalizedDni, String submittedFirstName,
                                           String submittedLastName, String submittedEmail,
                                           LocalDate requestedDateFrom, LocalDate requestedDateTo) {
                createCalls.incrementAndGet();
                throw new DataIntegrityViolationException("dni único");
            }

            @Override
            public WaitingListEntry registerWithExisting(String normalizedDni, String submittedFirstName,
                                                         String submittedLastName, String submittedEmail,
                                                         LocalDate requestedDateFrom, LocalDate requestedDateTo) {
                retryCalls.incrementAndGet();
                return waitingEntry;
            }
        };
        WaitingListRegistrationService registrationService = new WaitingListRegistrationService(transactions);

        WaitingListEntry result = registrationService.register(
            "27.123.456", firstName, lastName, "  Paciente.Ficticio@Example.COM  ", from, to);

        assertThat(result).isSameAs(waitingEntry);
        assertThat(existing.getFirstName()).isEqualTo("Perfil Original");
        assertThat(existing.getLastName()).isEqualTo("Apellido Original");
        assertThat(existing.getEmail()).isEqualTo("original@example.com");
        assertThat(createCalls).hasValue(1);
        assertThat(retryCalls).hasValue(1);
    }
}
