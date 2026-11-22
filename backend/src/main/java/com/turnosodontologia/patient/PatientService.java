package com.turnosodontologia.patient;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public PatientUpsertResult upsert(String dni, String firstName, String lastName, String email) {
        Optional<Patient> existing = patientRepository.findByDni(normalizedDni(dni));
        if (existing.isPresent()) {
            try {
                existing.get().updateProfile(firstName, lastName, email);
            } catch (IllegalArgumentException exception) {
                throw new InvalidPatientDataException();
            }
            return new PatientUpsertResult(existing.get(), false);
        }

        try {
            Patient saved = patientRepository.save(new Patient(dni, firstName, lastName, email));
            return new PatientUpsertResult(saved, true);
        } catch (IllegalArgumentException exception) {
            throw new InvalidPatientDataException();
        }
    }

    @Transactional(readOnly = true)
    public PatientPageResponse list(String dni, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (dni == null || dni.isBlank()) {
            return PatientPageResponse.from(patientRepository.findAll(pageable).map(PatientResponse::from));
        }

        Page<PatientResponse> found = patientRepository.findByDni(normalizedDni(dni))
            .map(patient -> new PageImpl<>(List.of(PatientResponse.from(patient)), pageable, 1))
            .orElseGet(() -> new PageImpl<>(List.of(), pageable, 0));
        return PatientPageResponse.from(found);
    }

    private String normalizedDni(String dni) {
        try {
            return Patient.normalizeDni(dni);
        } catch (IllegalArgumentException exception) {
            throw new InvalidPatientDataException();
        }
    }
}
