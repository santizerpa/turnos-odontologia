package com.turnosodontologia.seed;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentStatus;
import com.turnosodontologia.appointment.AppointmentTokenService;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.outbox.OutboxMessage;
import com.turnosodontologia.outbox.OutboxMessageType;
import com.turnosodontologia.outbox.OutboxStatus;
import com.turnosodontologia.outbox.ReminderGenerationResult;
import com.turnosodontologia.outbox.ReminderGenerationService;
import com.turnosodontologia.outbox.ReminderPerAppointmentTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DemoDataLoaderOutboxTests extends DemoLoaderTestSupport {

    @Test
    void demoMessagesArePendingBookingMessagesWithInvalidRecipients() {
        Dentist dentist = createDentist();

        loader().loadOrReuse(dentist);

        List<OutboxMessage> messages = outbox.findAll();
        assertThat(messages).hasSize(3);
        assertThat(messages).allSatisfy(message -> {
            assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(message.getType()).isEqualTo(OutboxMessageType.BOOKING);
            assertThat(message.getRecipientEmail()).endsWith(".invalid");
            assertThat(message.getSentAt()).isNull();
        });
        assertThat(messages).filteredOn(message -> message.getType() == OutboxMessageType.REMINDER).isEmpty();
        assertThat(messages).allSatisfy(message ->
            assertThat(outbox.existsByAppointmentIdAndType(message.getAppointment().getId(),
                OutboxMessageType.BOOKING)).isTrue());
    }

    @Test
    void demoMessagesLookLikeRealBookingMessagesWithWorkingLinks() {
        Dentist dentist = createDentist();

        loader().loadOrReuse(dentist);

        AppointmentTokenService tokenService = new AppointmentTokenService(tokens, Clock.fixed(NOW, ZONE));
        Pattern link = Pattern.compile("/api/public/appointments/([A-Za-z0-9_-]+)/confirm");
        assertThat(outbox.findAll()).hasSize(3).allSatisfy(message -> {
            assertThat(message.getSubject()).isEqualTo("Confirma tu turno");
            assertThat(message.getSubject()).doesNotContainIgnoringCase("demo");
            Matcher matcher = link.matcher(message.getBody());
            assertThat(matcher.find()).isTrue();
            String token = matcher.group(1);
            assertThat(message.getBody()).contains("/api/public/appointments/" + token + "/cancel");
            assertThat(tokenService.resolve(token).getAppointment().getId())
                .isEqualTo(message.getAppointment().getId());
        });
    }

    @Test
    void eachAppointmentHasAtMostOneBookingFixture() {
        Dentist dentist = createDentist();

        loader().loadOrReuse(dentist);

        assertThat(outbox.findAll())
            .extracting(message -> message.getAppointment().getId())
            .doesNotHaveDuplicates();
    }

    @Test
    void bookingFixtureCoexistsWithAReminderGeneratedForTheSameAppointment() {
        Dentist dentist = createDentist();
        loader().loadOrReuse(dentist);

        Appointment eligible = appointments.findAll().stream()
            .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
            .filter(appointment -> appointment.getStartAt().isAfter(NOW.plusSeconds(24 * 3600)))
            .filter(appointment -> !appointment.getStartAt().isAfter(NOW.plusSeconds(48 * 3600)))
            .findFirst().orElseThrow();

        Clock clock = Clock.fixed(NOW, ZONE);
        AppointmentTokenService tokenService = new AppointmentTokenService(tokens, clock);
        ReminderGenerationService reminders = new ReminderGenerationService(
            appointments, outbox, new ReminderPerAppointmentTransaction(tokenService, outbox), clock);

        ReminderGenerationResult result = reminders.generate();

        assertThat(result.created()).isEqualTo(1);
        UUID appointmentId = eligible.getId();
        assertThat(outbox.existsByAppointmentIdAndType(appointmentId, OutboxMessageType.BOOKING)).isTrue();
        assertThat(outbox.existsByAppointmentIdAndType(appointmentId, OutboxMessageType.REMINDER)).isTrue();
        assertThat(outbox.findAll()).hasSize(4);
        assertThat(outbox.findAll()).filteredOn(message -> message.getType() == OutboxMessageType.BOOKING)
            .hasSize(3);
        assertThat(outbox.findAll()).filteredOn(message -> message.getType() == OutboxMessageType.REMINDER)
            .hasSize(1);
        assertThat(outbox.findAll()).allSatisfy(message ->
            assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING));
    }
}
