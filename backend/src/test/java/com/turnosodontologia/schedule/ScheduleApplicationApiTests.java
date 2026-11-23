package com.turnosodontologia.schedule;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.schedule.dto.*;
import com.turnosodontologia.schedule.error.ScheduleValidationException;
import com.turnosodontologia.schedule.config.ScheduleProperties;
import com.turnosodontologia.schedule.config.ScheduleClockConfiguration;
import com.turnosodontologia.support.AuthFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@EnableConfigurationProperties(ScheduleProperties.class)
@Import({ScheduleConfigurationService.class, ScheduleBlockService.class, ScheduleClockConfiguration.class})
class ScheduleApplicationApiTests {
    @Autowired DentistRepository dentists;
    @Autowired ScheduleConfigurationRepository configurations;
    @Autowired ScheduleConfigurationService configurationService;
    @Autowired ScheduleBlockService blockService;

    @Test
    void getAndPatchReplaceConfigurationWithoutMutatingOnValidationError() {
        Dentist dentist = saveDentist("api.config@odontologia.invalid");
        ScheduleConfigurationResponse before = configurationService.get(dentist);
        assertThat(before.timezone()).isEqualTo("America/Argentina/Buenos_Aires");
        assertThat(before.slotDurationMinutes()).isEqualTo(30);
        assertThat(before.publicWindow().minNoticeHours()).isEqualTo(24);

        SchedulePatchRequest patch = validPatch();
        ScheduleConfigurationResponse replaced = configurationService.patch(dentist, patch);
        assertThat(replaced.days()).allSatisfy(day -> {
            if (day.day().name().equals("FRIDAY")) assertThat(day.windows()).isEmpty();
        });

        assertThatThrownBy(() -> configurationService.patch(dentist, patchWithInvertedWindow()))
                .isInstanceOf(ScheduleValidationException.class);
        assertThat(configurationService.get(dentist).days()).isEqualTo(replaced.days());
    }

    @Test
    void patchRejectsAllInvalidWeekShapesAndKeepsPreviousState() {
        Dentist dentist = saveDentist("api.validation@odontologia.invalid");
        ScheduleConfigurationResponse before = configurationService.get(dentist);

        assertThatThrownBy(() -> configurationService.patch(dentist, patchWithShortWindow())).isInstanceOf(ScheduleValidationException.class);
        assertThatThrownBy(() -> configurationService.patch(dentist, patchWithOverlap())).isInstanceOf(ScheduleValidationException.class);
        assertThatThrownBy(() -> configurationService.patch(dentist, patchMissingDay())).isInstanceOf(ScheduleValidationException.class);
        assertThatThrownBy(() -> configurationService.patch(dentist, patchRepeatedDay())).isInstanceOf(ScheduleValidationException.class);
        assertThatThrownBy(() -> configurationService.patch(dentist, patchWeekend())).isInstanceOf(ScheduleValidationException.class);
        assertThatThrownBy(() -> configurationService.patch(dentist, patchWrongTimezone())).isInstanceOf(ScheduleValidationException.class);

        assertThat(configurationService.get(dentist).days()).isEqualTo(before.days());
    }

    @Test
    void blockCreateSoftDeleteAndOwnershipRules() {
        Dentist owner = saveDentist("api.owner@odontologia.invalid");
        Dentist other = saveDentist("api.other@odontologia.invalid");

        ScheduleBlockResponse created = blockService.create(owner.getId(), new ScheduleBlockCreateRequest(
                OffsetDateTime.parse("2027-01-10T10:00:00-03:00"),
                OffsetDateTime.parse("2027-01-10T11:00:00-03:00"), "Ficticio"));
        assertThat(created.reason()).isEqualTo("Ficticio");

        blockService.delete(owner.getId(), created.id());
        Instant deletedAt = blockService.find(created.id()).getDeletedAt();
        blockService.delete(owner.getId(), created.id());
        assertThat(blockService.find(created.id()).getDeletedAt()).isEqualTo(deletedAt);

        assertThatThrownBy(() -> blockService.delete(other.getId(), created.id())).isInstanceOf(com.turnosodontologia.schedule.error.ScheduleNotFoundException.class);
        assertThatThrownBy(() -> blockService.delete(owner.getId(), java.util.UUID.randomUUID())).isInstanceOf(com.turnosodontologia.schedule.error.ScheduleNotFoundException.class);
        assertThatThrownBy(() -> blockService.create(owner.getId(), new ScheduleBlockCreateRequest(
                OffsetDateTime.parse("2027-01-10T11:00:00-03:00"),
                OffsetDateTime.parse("2027-01-10T10:00:00-03:00"), null)))
                .isInstanceOf(com.turnosodontologia.schedule.error.ScheduleValidationException.class);
    }

    private Dentist saveDentist(String email) {
        return dentists.save(new Dentist(email, new BCryptPasswordEncoder().encode("ficticia"), "Ficticio", "Odontologo"));
    }

    private SchedulePatchRequest validPatch() {
        return new SchedulePatchRequest(null, List.of(
                day("MONDAY", "10:00", "12:00", "FRIDAY"),
                day("TUESDAY", "10:00", "12:00", null),
                day("WEDNESDAY", "10:00", "12:00", null),
                day("THURSDAY", "10:00", "12:00", null),
                day("FRIDAY", null, null, null)));
    }

    private ScheduleDayRequest day(String day, String start, String end, String ignored) {
        List<ScheduleWindowRequest> windows = start == null ? List.of() : List.of(new ScheduleWindowRequest(start, end));
        return new ScheduleDayRequest(day, windows);
    }

    private SchedulePatchRequest patchWithInvertedWindow() { return new SchedulePatchRequest(null, List.of(
            day("MONDAY", "12:00", "10:00", null), day("TUESDAY", null, null, null), day("WEDNESDAY", null, null, null), day("THURSDAY", null, null, null), day("FRIDAY", null, null, null))); }
    private SchedulePatchRequest patchWithShortWindow() { return new SchedulePatchRequest(null, List.of(
            day("MONDAY", "09:00", "09:20", null), day("TUESDAY", null, null, null), day("WEDNESDAY", null, null, null), day("THURSDAY", null, null, null), day("FRIDAY", null, null, null))); }
    private SchedulePatchRequest patchWithOverlap() { return new SchedulePatchRequest(null, List.of(
            new ScheduleDayRequest("MONDAY", List.of(new ScheduleWindowRequest("09:00", "11:00"), new ScheduleWindowRequest("10:30", "12:00"))),
            day("TUESDAY", null, null, null), day("WEDNESDAY", null, null, null), day("THURSDAY", null, null, null), day("FRIDAY", null, null, null))); }
    private SchedulePatchRequest patchMissingDay() { return new SchedulePatchRequest(null, List.of(day("MONDAY", null, null, null), day("TUESDAY", null, null, null), day("WEDNESDAY", null, null, null), day("THURSDAY", null, null, null))); }
    private SchedulePatchRequest patchRepeatedDay() { return new SchedulePatchRequest(null, List.of(day("MONDAY", null, null, null), day("MONDAY", null, null, null), day("TUESDAY", null, null, null), day("WEDNESDAY", null, null, null), day("THURSDAY", null, null, null))); }
    private SchedulePatchRequest patchWeekend() { return new SchedulePatchRequest(null, List.of(day("MONDAY", null, null, null), day("TUESDAY", null, null, null), day("WEDNESDAY", null, null, null), day("THURSDAY", null, null, null), day("SATURDAY", null, null, null))); }
    private SchedulePatchRequest patchWrongTimezone() { return new SchedulePatchRequest("Europe/Madrid", List.of(day("MONDAY", null, null, null), day("TUESDAY", null, null, null), day("WEDNESDAY", null, null, null), day("THURSDAY", null, null, null), day("FRIDAY", null, null, null))); }
}
