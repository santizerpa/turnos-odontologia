package com.turnosodontologia.waiting;

import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class WaitingListRegistrationTransaction {

    private final PatientRepository patients;
    private final WaitingListEntryRepository waitingEntries;

    public WaitingListRegistrationTransaction(PatientRepository patients,
                                              WaitingListEntryRepository waitingEntries) {
        this.patients = patients;
        this.waitingEntries = waitingEntries;
    }

    @Transactional
    public WaitingListEntry create(String normalizedDni, String firstName, String lastName, String email,
                                   LocalDate requestedDateFrom, LocalDate requestedDateTo) {
        Patient patient = patients.findByDni(normalizedDni)
            .orElseGet(() -> patients.saveAndFlush(new Patient(normalizedDni, firstName, lastName, email)));
        var existing = waitingEntries.findByPatientIdAndStatus(patient.getId(), WaitingListStatus.WAITING);
        if (existing.isPresent()) return existing.get();
        return waitingEntries.save(new WaitingListEntry(
            patient, requestedDateFrom, requestedDateTo, WaitingListStatus.WAITING));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WaitingListEntry registerWithExisting(String normalizedDni, String firstName, String lastName,
                                                 String email, LocalDate requestedDateFrom,
                                                 LocalDate requestedDateTo) {
        Patient patient = patients.findByDni(normalizedDni)
            .orElseThrow(() -> new DataIntegrityViolationException("No se encontró el paciente concurrente."));
        var existing = waitingEntries.findByPatientIdAndStatus(patient.getId(), WaitingListStatus.WAITING);
        if (existing.isPresent()) return existing.get();
        return waitingEntries.save(new WaitingListEntry(
            patient, requestedDateFrom, requestedDateTo, WaitingListStatus.WAITING));
    }
}
