package com.turnosodontologia.appointment;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.patient.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");

    @Mock AppointmentRepository appointments;
    @Mock AppointmentEventRepository appointmentEvents;
    @Mock ApplicationEventPublisher eventPublisher;

    private AppointmentService service;
    private Dentist dentist;
    private Patient patient;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointments, appointmentEvents, eventPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC));
        dentist = new Dentist("ana.demo@example.invalid", "hash-ficticio", "Ana", "Demo");
        patient = new Patient("12345678", "Paciente", "Ficticio", "paciente@example.invalid");
    }

    @Test
    void creationIsThirtyMinutesWithStartAsActiveSlot() {
        Instant start = NOW.plusSeconds(48 * 60 * 60);

        Appointment appointment = Appointment.create(dentist, patient, start, 30, CreatedBy.PATIENT);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getDurationMinutes()).isEqualTo(30);
        assertThat(appointment.getActiveSlot()).isEqualTo(start);
    }

    @Test
    void rejectsDurationOtherThanThirtyMinutes() {
        assertThatThrownBy(() -> Appointment.create(dentist, patient, NOW.plusSeconds(3600), 20, CreatedBy.PATIENT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Duración fija de 30 minutos");
    }

    @Test
    void validStateMatrixTransitionsWithoutInvalidMutation() {
        for (AppointmentStatus from : new AppointmentStatus[]{AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED}) {
            for (AppointmentStatus to : matrixTargets(from)) {
                Appointment appointment = appointmentAtStatus(from);
                UUID id = UUID.fromString("00000000-0000-0000-0000-0000000000" + (from.ordinal() + 1));
                setId(appointment, id);
                when(appointments.findById(id)).thenReturn(Optional.of(appointment));

                service.changeStatus(id, to, Actor.DENTIST, "motivo ficticio");

                assertThat(appointment.getStatus()).isEqualTo(to);
            }
        }

        Appointment cancelled = appointmentAtStatus(AppointmentStatus.CANCELLED);
        UUID cancelledId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        setId(cancelled, cancelledId);
        when(appointments.findById(cancelledId)).thenReturn(Optional.of(cancelled));

        org.mockito.Mockito.clearInvocations(appointmentEvents);
        assertThatThrownBy(() -> service.changeStatus(cancelledId, AppointmentStatus.PENDING, Actor.DENTIST, "volver"))
            .isInstanceOf(IllegalStateException.class);
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentEvents, never()).save(any(AppointmentEvent.class));
    }

    @Test
    void patientCanCancelAtLeastTwentyFourHoursBeforeStart() {
        Appointment appointment = appointmentAtStatus(AppointmentStatus.PENDING);
        setId(appointment, UUID.fromString("00000000-0000-0000-0000-000000000101"));
        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        CancelAppointmentResult result = service.cancel(appointment.getId(), Actor.PATIENT, "paciente ficticio");

        assertThat(result.cancelled()).isTrue();
        assertThat(result.message()).isNotEqualTo("ya cancelado");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getActiveSlot()).isNull();
        verify(appointmentEvents).save(any(AppointmentEvent.class));
        verify(eventPublisher).publishEvent(any(AppointmentCancelledEvent.class));
    }

    @Test
    void patientCancelAtExactlyTwentyFourHoursIsAllowed() {
        Appointment appointment = appointmentAtStatus(AppointmentStatus.CONFIRMED);
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000103");
        setId(appointment, id);
        org.springframework.test.util.ReflectionTestUtils.setField(appointment, "startAt", NOW.plusSeconds(24 * 60 * 60));
        when(appointments.findById(id)).thenReturn(Optional.of(appointment));

        CancelAppointmentResult result = service.cancel(id, Actor.PATIENT, "exactamente 24 horas");

        assertThat(result.cancelled()).isTrue();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void patientCancelBeforeTwentyFourHoursIsRejectedWithoutMutation() {
        Appointment appointment = appointmentAtStatus(AppointmentStatus.CONFIRMED);
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000104");
        setId(appointment, id);
        org.springframework.test.util.ReflectionTestUtils.setField(appointment, "startAt", NOW.plusSeconds(24 * 60 * 60 - 1));
        when(appointments.findById(id)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.cancel(id, Actor.PATIENT, "fuera de plazo"))
            .isInstanceOf(IllegalStateException.class);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getActiveSlot()).isEqualTo(NOW.plusSeconds(48 * 60 * 60));
    }

    @Test
    void dentistCancelAndRepeatedCancelAreIdempotent() {
        Appointment appointment = appointmentAtStatus(AppointmentStatus.CONFIRMED);
        setId(appointment, UUID.fromString("00000000-0000-0000-0000-000000000102"));
        when(appointments.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        CancelAppointmentResult first = service.cancel(appointment.getId(), Actor.DENTIST, "agenda ficticia");
        CancelAppointmentResult second = service.cancel(appointment.getId(), Actor.DENTIST, "agenda ficticia");

        assertThat(first.cancelled()).isTrue();
        assertThat(second.cancelled()).isFalse();
        assertThat(second.message()).isEqualTo("ya cancelado");
        assertThat(appointment.getActiveSlot()).isNull();
        verify(appointmentEvents).save(any(AppointmentEvent.class));
        verify(eventPublisher).publishEvent(any(AppointmentCancelledEvent.class));
    }

    private AppointmentStatus[] matrixTargets(AppointmentStatus from) {
        if (from == AppointmentStatus.PENDING) {
            return new AppointmentStatus[]{AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED, AppointmentStatus.ABSENT};
        }
        if (from == AppointmentStatus.CONFIRMED) {
            return new AppointmentStatus[]{AppointmentStatus.CANCELLED, AppointmentStatus.ABSENT, AppointmentStatus.ATTENDED};
        }
        return new AppointmentStatus[0];
    }

    private Appointment appointmentAtStatus(AppointmentStatus status) {
        Appointment appointment = Appointment.create(dentist, patient, NOW.plusSeconds(48 * 60 * 60), 30, CreatedBy.PATIENT);
        if (status != AppointmentStatus.PENDING) {
            while (appointment.getStatus() != status) {
                if (appointment.getStatus() == AppointmentStatus.PENDING && status == AppointmentStatus.CONFIRMED) {
                    appointment.confirmForFutureDomainService();
                } else {
                    break;
                }
            }
        }
        if (status == AppointmentStatus.CANCELLED) {
            appointment.cancelForFutureDomainService(NOW);
        }
        if (status == AppointmentStatus.ABSENT || status == AppointmentStatus.ATTENDED) {
            if (status == AppointmentStatus.ATTENDED) appointment.confirmForFutureDomainService();
            // El estado de estas trayectorias se prueba mediante el servicio.
        }
        return appointment;
    }

    private void setId(Appointment appointment, UUID id) {
        org.springframework.test.util.ReflectionTestUtils.setField(appointment, "id", id);
    }
}
