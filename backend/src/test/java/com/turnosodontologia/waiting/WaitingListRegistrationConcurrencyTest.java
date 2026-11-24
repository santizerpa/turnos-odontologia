package com.turnosodontologia.waiting;

import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WaitingListRegistrationConcurrencyTest {

    @Autowired
    WaitingListRegistrationService registrationService;

    @Autowired
    PatientRepository patients;

    @Autowired
    WaitingListEntryRepository waitingEntries;

    @Test
    void concurrentRegistrationsWithEquivalentDniReuseOnePatient() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        LocalDate from = LocalDate.of(2030, 12, 10);
        LocalDate to = LocalDate.of(2030, 12, 20);

        try {
            Future<WaitingListEntry> first = executor.submit(() -> registerWhenReleased(
                "27.123.456", "Paciente Uno", "Ficticio", "uno@example.com", from, to, ready, start));
            Future<WaitingListEntry> second = executor.submit(() -> registerWhenReleased(
                "27123456", "Paciente Dos", "Ficticio", "dos@example.com", from, to, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            WaitingListEntry firstEntry = first.get(10, TimeUnit.SECONDS);
            WaitingListEntry secondEntry = second.get(10, TimeUnit.SECONDS);

            assertThat(firstEntry.getPatient().getId()).isEqualTo(secondEntry.getPatient().getId());
            assertThat(patients.findByDni("27123456")).isPresent();
            assertThat(waitingEntries.findAll().stream()
                .filter(entry -> entry.getPatient().getId().equals(firstEntry.getPatient().getId())))
                .hasSize(2);
        } finally {
            executor.shutdownNow();
        }
    }

    private WaitingListEntry registerWhenReleased(String dni, String firstName, String lastName, String email,
                                                  LocalDate from, LocalDate to, CountDownLatch ready,
                                                  CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("No se liberó la barrera de registro.");
            }
            return registrationService.register(dni, firstName, lastName, email, from, to);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Hilo de registro interrumpido.", exception);
        }
    }
}
