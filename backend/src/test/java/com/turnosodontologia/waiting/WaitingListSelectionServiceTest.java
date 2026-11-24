package com.turnosodontologia.waiting;

import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WaitingListSelectionServiceTest {

    @Autowired
    WaitingListSelectionService selectionService;

    @Autowired
    PatientRepository patients;

    @Autowired
    WaitingListEntryRepository waitingEntries;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    void selectsOldestEligibleWaitingEntrySkippingOlderIneligibleAndNotifiedEntries() {
        LocalDate releasedDate = LocalDate.of(2040, 9, 12);
        WaitingListEntry outsideRange = entry("80123456", "Fuera", "de rango",
            releasedDate.minusDays(3), releasedDate.minusDays(1), WaitingListStatus.WAITING);
        WaitingListEntry notified = entry("80123457", "Ya", "avisado",
            releasedDate, releasedDate, WaitingListStatus.NOTIFIED);
        WaitingListEntry oldestEligible = entry("80123458", "Primero", "elegible",
            releasedDate, releasedDate.plusDays(2), WaitingListStatus.WAITING);
        WaitingListEntry laterEligible = entry("80123459", "Segundo", "elegible",
            releasedDate.minusDays(1), releasedDate, WaitingListStatus.WAITING);
        setCreatedAt(outsideRange, "2040-01-01T10:00:00Z");
        setCreatedAt(notified, "2040-01-02T10:00:00Z");
        setCreatedAt(oldestEligible, "2040-01-03T10:00:00Z");
        setCreatedAt(laterEligible, "2040-01-04T10:00:00Z");

        WaitingListEntry selected = selectionService.selectFirstEligible(releasedDate).orElseThrow();

        assertThat(selected.getId()).isEqualTo(oldestEligible.getId());
        assertThat(outsideRange.getStatus()).isEqualTo(WaitingListStatus.WAITING);
        assertThat(notified.getStatus()).isEqualTo(WaitingListStatus.NOTIFIED);
    }

    @Test
    void convertsAppointmentInstantToConfiguredArgentinaLocalDate() {
        LocalDate argentinaDate = LocalDate.of(2040, 9, 11);
        LocalDate followingUtcDate = LocalDate.of(2040, 9, 12);
        WaitingListEntry argentinaMatch = entry("84123456", "Fecha", "Argentina",
            argentinaDate, argentinaDate, WaitingListStatus.WAITING);
        entry("84123457", "Fecha", "UTC", followingUtcDate, followingUtcDate, WaitingListStatus.WAITING);

        WaitingListEntry selected = selectionService
            .selectFirstEligible(Instant.parse("2040-09-12T02:30:00Z"))
            .orElseThrow();

        assertThat(selected.getId()).isEqualTo(argentinaMatch.getId());
    }

    @Test
    void transitionsOnlyTheSelectedWaitingEntryAndPersistsTheState() {
        LocalDate releasedDate = LocalDate.of(2040, 10, 5);
        WaitingListEntry eligible = entry("83123456", "Elegible", "Ficticio",
            releasedDate, releasedDate, WaitingListStatus.WAITING);
        WaitingListEntry outside = entry("83123457", "Fuera", "de rango",
            releasedDate.plusDays(1), releasedDate.plusDays(2), WaitingListStatus.WAITING);
        WaitingListEntry alreadyNotified = entry("83123458", "Ya", "avisado",
            releasedDate, releasedDate, WaitingListStatus.NOTIFIED);

        WaitingListEntry selected = selectionService.selectFirstEligible(releasedDate).orElseThrow();
        assertThat(selected.getId()).isEqualTo(eligible.getId());
        assertThat(selected.markNotified()).isTrue();
        assertThat(alreadyNotified.markNotified()).isFalse();
        waitingEntries.flush();
        entityManager.clear();

        assertThat(waitingEntries.findById(eligible.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.NOTIFIED);
        assertThat(waitingEntries.findById(outside.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.WAITING);
        assertThat(waitingEntries.findById(alreadyNotified.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.NOTIFIED);
    }

    @Test
    void breaksEqualCreationTimestampsById() {
        LocalDate releasedDate = LocalDate.of(2040, 10, 5);
        WaitingListEntry firstInserted = entry("82123456", "Primero", "insertado",
            null, null, WaitingListStatus.WAITING);
        WaitingListEntry secondInserted = entry("82123457", "Segundo", "insertado",
            null, null, WaitingListStatus.WAITING);
        setCreatedAt(firstInserted, "2040-01-01T10:00:00Z");
        setCreatedAt(secondInserted, "2040-01-01T10:00:00Z");
        WaitingListEntry expected = firstInserted.getId().toString().compareTo(secondInserted.getId().toString()) < 0
            ? firstInserted : secondInserted;

        WaitingListEntry selected = selectionService.selectFirstEligible(releasedDate).orElseThrow();

        assertThat(selected.getId()).isEqualTo(expected.getId());
    }

    @Test
    void acceptsInclusiveBoundariesAndEitherNullDateLimit() {
        LocalDate releasedDate = LocalDate.of(2040, 10, 5);
        WaitingListEntry nullStart = entry("81123456", "Sin", "inicio",
            null, releasedDate, WaitingListStatus.WAITING);
        WaitingListEntry nullEnd = entry("81123457", "Sin", "fin",
            releasedDate, null, WaitingListStatus.WAITING);
        setCreatedAt(nullStart, "2040-01-01T10:00:00Z");
        setCreatedAt(nullEnd, "2040-01-02T10:00:00Z");

        List<WaitingListEntry> eligible = waitingEntries.findEligibleWaitingEntriesForUpdate(
            WaitingListStatus.WAITING, releasedDate, null);

        assertThat(eligible).extracting(WaitingListEntry::getId)
            .containsExactly(nullStart.getId(), nullEnd.getId());
        assertThat(selectionService.selectFirstEligible(releasedDate)).contains(nullStart);
    }

    private WaitingListEntry entry(String dni, String firstName, String lastName,
                                   LocalDate from, LocalDate to, WaitingListStatus status) {
        Patient patient = patients.saveAndFlush(new Patient(dni, firstName, lastName,
            firstName.toLowerCase() + "@example.invalid"));
        return waitingEntries.saveAndFlush(new WaitingListEntry(patient, from, to, status));
    }

    private void setCreatedAt(WaitingListEntry entry, String instant) {
        jdbcTemplate.update("UPDATE waiting_list_entries SET created_at = ? WHERE id = ?",
            Timestamp.from(Instant.parse(instant)), entry.getId());
    }
}
