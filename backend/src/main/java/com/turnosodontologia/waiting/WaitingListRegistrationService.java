package com.turnosodontologia.waiting;

import com.turnosodontologia.patient.Patient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class WaitingListRegistrationService {

    private final WaitingListRegistrationTransaction transactions;

    public WaitingListRegistrationService(WaitingListRegistrationTransaction transactions) {
        this.transactions = transactions;
    }

    public WaitingListEntry register(String dni, String firstName, String lastName, String email,
                                     LocalDate requestedDateFrom, LocalDate requestedDateTo) {
        String normalizedDni;
        String normalizedEmail;
        try {
            normalizedDni = Patient.normalizeDni(dni);
            normalizedEmail = Patient.normalizeEmail(email);
        } catch (IllegalArgumentException exception) {
            throw new InvalidWaitingListDataException();
        }
        if (!isValidName(firstName) || !isValidName(lastName)
            || (requestedDateFrom != null && requestedDateTo != null
                && requestedDateFrom.isAfter(requestedDateTo))) {
            throw new InvalidWaitingListDataException();
        }

        try {
            return transactions.create(normalizedDni, firstName, lastName, normalizedEmail,
                requestedDateFrom, requestedDateTo);
        } catch (DataIntegrityViolationException concurrentPatientInsert) {
            return transactions.registerWithExisting(normalizedDni, firstName, lastName, normalizedEmail,
                requestedDateFrom, requestedDateTo);
        }
    }

    private boolean isValidName(String name) {
        return name != null && !name.isBlank() && name.length() <= 100;
    }
}
