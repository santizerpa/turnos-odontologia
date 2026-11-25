package com.turnosodontologia.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@Validated
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public PatientPageResponse list(@RequestParam(required = false) String dni,
                                    @RequestParam(required = false) String nombre,
                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return patientService.list(dni, nombre, page, size);
    }

    @PostMapping
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        PatientUpsertResult result = patientService.upsert(
            request.dni(), request.nombre(), request.apellido(), request.email());

        return result.created()
            ? ResponseEntity.status(HttpStatus.CREATED).body(PatientResponse.from(result.patient()))
            : ResponseEntity.ok(PatientResponse.from(result.patient()));
    }
}
