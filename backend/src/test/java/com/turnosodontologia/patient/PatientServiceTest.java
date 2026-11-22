package com.turnosodontologia.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    private static final UUID PACIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    PatientRepository patientRepository;

    @InjectMocks
    PatientService patientService;

    @Test
    void returnsEmptyPageWhenDniHasNoMatch() {
        when(patientRepository.findByDni("27123456")).thenReturn(Optional.empty());

        PatientPageResponse page = patientService.list("27123456", 0, 5);

        assertAll(
            () -> assertThat(page.content()).isEmpty(),
            () -> assertThat(page.totalElements()).isZero(),
            () -> assertThat(page.page()).isZero(),
            () -> assertThat(page.size()).isEqualTo(5)
        );
        verify(patientRepository).findByDni("27123456");
    }

    @Test
    void rejectsInvalidDniFilterWithoutQueryingRepository() {
        assertThatThrownBy(() -> patientService.list("27123A56", 0, 10))
            .isInstanceOf(InvalidPatientDataException.class);

        verify(patientRepository, never()).findByDni(anyString());
        verify(patientRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listsWithoutFilterUsingPageable() {
        Patient one = withId(new Patient("27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"),
            PACIENTE_ID);
        when(patientRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(one), PageRequest.of(0, 2), 1));

        PatientPageResponse page = patientService.list(null, 0, 2);

        assertAll(
            () -> assertThat(page.content()).hasSize(1),
            () -> assertThat(page.content().get(0).dni()).isEqualTo("27123456"),
            () -> assertThat(page.totalElements()).isEqualTo(1),
            () -> assertThat(page.size()).isEqualTo(2)
        );
        verify(patientRepository).findAll(any(Pageable.class));
    }

    @Test
    void findsPatientByFormattedDni() {
        Patient existing = withId(
            new Patient("27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"), PACIENTE_ID);
        when(patientRepository.findByDni("27123456")).thenReturn(Optional.of(existing));

        PatientPageResponse page = patientService.list("27.123.456", 0, 10);

        assertAll(
            () -> assertThat(page.content()).hasSize(1),
            () -> assertThat(page.content().get(0).dni()).isEqualTo("27123456"),
            () -> assertThat(page.content().get(0).id()).isEqualTo(PACIENTE_ID),
            () -> assertThat(page.totalElements()).isEqualTo(1)
        );
        verify(patientRepository).findByDni("27123456");
    }

    @Test
    void createsPatientWithNormalizedDni() {
        when(patientRepository.findByDni("27123456")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class)))
            .thenAnswer(invocation -> withId(invocation.getArgument(0), PACIENTE_ID));

        PatientUpsertResult result = patientService.upsert(
            "27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com");

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        assertThat(result.created()).isTrue();
        assertThat(result.patient().getDni()).isEqualTo("27123456");
        assertThat(result.patient().getId()).isEqualTo(PACIENTE_ID);
        assertThat(captor.getValue().getDni()).isEqualTo("27123456");
    }

    @Test
    void updatesExistingPatientOnSameIdentity() {
        Patient existing = withId(
            new Patient("27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"), PACIENTE_ID);
        when(patientRepository.findByDni("27123456")).thenReturn(Optional.of(existing));

        PatientUpsertResult result = patientService.upsert("27123456", "Paciente Actualizado",
            "Ficticio Actualizado", "  Nuevo.Ficticio@Consultorio.Example.COM  ");

        assertAll(
            () -> assertThat(result.created()).isFalse(),
            () -> assertThat(result.patient().getId()).isEqualTo(PACIENTE_ID),
            () -> assertThat(result.patient().getDni()).isEqualTo("27123456"),
            () -> assertThat(result.patient().getFirstName()).isEqualTo("Paciente Actualizado"),
            () -> assertThat(result.patient().getLastName()).isEqualTo("Ficticio Actualizado"),
            () -> assertThat(result.patient().getEmail()).isEqualTo("nuevo.ficticio@consultorio.example.com")
        );
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void rejectsInvalidEmailOnUpsertWithoutMutatingExistingPatient() {
        Patient existing = withId(
            new Patient("27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"), PACIENTE_ID);
        when(patientRepository.findByDni("27123456")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> patientService.upsert(
            "27123456", "Cambio", "Cambio", "paciente.ficticio@ejemplo"))
            .isInstanceOf(InvalidPatientDataException.class);

        assertAll(
            () -> assertThat(existing.getId()).isEqualTo(PACIENTE_ID),
            () -> assertThat(existing.getDni()).isEqualTo("27123456"),
            () -> assertThat(existing.getFirstName()).isEqualTo("Paciente"),
            () -> assertThat(existing.getLastName()).isEqualTo("Ficticio"),
            () -> assertThat(existing.getEmail()).isEqualTo("paciente.ficticio@example.com")
        );
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"27123A56", "271234", "271234567"})
    void rejectsInvalidDniWithoutPersisting(String dni) {
        assertThatThrownBy(() -> patientService.upsert(
            dni, "Paciente", "Ficticio", "paciente.ficticio@example.com"))
            .isInstanceOf(InvalidPatientDataException.class);

        verify(patientRepository, never()).findByDni(anyString());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void rejectsInvalidEmailWithoutPersisting() {
        assertThatThrownBy(() -> patientService.upsert(
            "27123456", "Paciente", "Ficticio", "paciente.ficticio@ejemplo"))
            .isInstanceOf(InvalidPatientDataException.class);

        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void reusesSameIdentityForFormattedDnis() {
        Patient existing = withId(
            new Patient("27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"), PACIENTE_ID);
        when(patientRepository.findByDni("27123456")).thenReturn(Optional.of(existing));

        PatientUpsertResult first = patientService.upsert(
            "27.123.456", "Paciente", "Ficticio", "paciente.ficticio@example.com");
        PatientUpsertResult second = patientService.upsert(
            "27 123 456", "Paciente", "Ficticio", "paciente.ficticio@example.com");

        assertAll(
            () -> assertThat(first.created()).isFalse(),
            () -> assertThat(second.created()).isFalse(),
            () -> assertThat(first.patient().getId()).isEqualTo(PACIENTE_ID),
            () -> assertThat(second.patient().getId()).isEqualTo(PACIENTE_ID),
            () -> assertThat(first.patient().getDni()).isEqualTo("27123456"),
            () -> assertThat(second.patient().getDni()).isEqualTo("27123456")
        );
        verify(patientRepository, times(2)).findByDni("27123456");
        verify(patientRepository, never()).save(any(Patient.class));
    }

    private static Patient withId(Patient patient, UUID id) {
        ReflectionTestUtils.setField(patient, "id", id);
        return patient;
    }
}
