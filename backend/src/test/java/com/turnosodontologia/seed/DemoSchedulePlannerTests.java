package com.turnosodontologia.seed;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoSchedulePlannerTests {
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private final DemoSchedulePlanner planner = new DemoSchedulePlanner();

    @Test
    void effectiveDateComesFromTheConfiguredZoneAndNotFromUtc() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-24T02:00:00Z"), ZONE);

        DemoSchedulePlan plan = planner.plan(clock.instant(), ZONE, defaultAgenda());

        assertThat(plan.effectiveDate()).isEqualTo(LocalDate.of(2026, 11, 23));
    }

    @Test
    void sameEffectiveDateProducesTheSamePlanRegardlessOfTimeOfDay() {
        Clock morning = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZONE);
        Clock evening = Clock.fixed(Instant.parse("2026-11-24T23:00:00Z"), ZONE);

        DemoSchedulePlan first = planner.plan(morning.instant(), ZONE, defaultAgenda());
        DemoSchedulePlan second = planner.plan(evening.instant(), ZONE, defaultAgenda());

        assertThat(first.effectiveDate()).isEqualTo(LocalDate.of(2026, 11, 24));
        assertThat(second).isEqualTo(first);
    }

    @Test
    void weekChangeSelectsConfiguredWeekdaysInsideThePublicWindow() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZONE);
        Instant now = clock.instant();

        DemoSchedulePlan plan = planner.plan(now, ZONE, defaultAgenda());

        assertThat(plan.effectiveDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(localDate(plan.blockStart())).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(plan.blockStart().atZone(ZONE).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        for (Instant start : plan.activeStarts()) {
            assertThat(start).isAfterOrEqualTo(now.plusSeconds(24 * 3600));
            assertThat(start).isBeforeOrEqualTo(now.plusSeconds(30 * 86_400L));
            assertThat(start.atZone(ZONE).getDayOfWeek())
                .isIn(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
            assertThat(isInsideAnyConfiguredWindow(start, defaultAgenda())).isTrue();
        }
    }

    @Test
    void activeAppointmentsDoNotOverlapTheBlockOrEachOther() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZONE);

        DemoSchedulePlan plan = planner.plan(clock.instant(), ZONE, defaultAgenda());
        List<Instant[]> activeIntervals = plan.activeStarts().stream()
            .map(start -> new Instant[]{start, start.plusSeconds(30 * 60L)})
            .toList();

        assertThat(plan.activeStarts()).doesNotHaveDuplicates();
        for (Instant[] interval : activeIntervals) {
            assertThat(overlaps(interval[0], interval[1], plan.blockStart(), plan.blockEnd())).isFalse();
        }
        for (int i = 0; i < activeIntervals.size(); i++) {
            for (int j = i + 1; j < activeIntervals.size(); j++) {
                assertThat(overlaps(activeIntervals.get(i)[0], activeIntervals.get(i)[1],
                    activeIntervals.get(j)[0], activeIntervals.get(j)[1])).isFalse();
            }
        }
        assertThat(plan.blockEnd()).isEqualTo(plan.blockStart().plusSeconds(30 * 60L));
    }

    @Test
    void inclusiveWindowBoundsAcceptExactlyTwentyFourHoursAndThirtyDays() {
        Instant now = Instant.parse("2026-11-24T12:00:00Z");

        assertThat(DemoSchedulePlanner.isWithinWindow(now.plusSeconds(24 * 3600), now)).isTrue();
        assertThat(DemoSchedulePlanner.isWithinWindow(now.plusSeconds(24 * 3600).minusSeconds(1), now)).isFalse();
        assertThat(DemoSchedulePlanner.isWithinWindow(now.plusSeconds(30 * 86_400L), now)).isTrue();
        assertThat(DemoSchedulePlanner.isWithinWindow(now.plusSeconds(30 * 86_400L).plusSeconds(1), now)).isFalse();
    }

    @Test
    void configuredDaysAndTimeWindowsAreRespected() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZONE);
        List<DemoWindow> agenda = List.of(
            new DemoWindow(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
            new DemoWindow(DayOfWeek.THURSDAY, LocalTime.of(13, 0), LocalTime.of(17, 0)));

        DemoSchedulePlan plan = planner.plan(clock.instant(), ZONE, agenda);

        List<Instant> starts = new ArrayList<>(plan.activeStarts());
        starts.add(plan.cancelledStart());
        starts.add(plan.blockStart());
        for (Instant start : starts) {
            ZonedDateTime local = start.atZone(ZONE);
            assertThat(local.getDayOfWeek()).isIn(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
            assertThat(isInsideAnyConfiguredWindow(start, agenda)).isTrue();
        }
    }

    @Test
    void insufficientAgendaFailsClearly() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZONE);
        List<DemoWindow> agenda = List.of(
            new DemoWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 30)));

        assertThatThrownBy(() -> planner.plan(clock.instant(), ZONE, agenda))
            .isInstanceOf(DemoSeedException.class)
            .hasMessageContaining("slots");
    }

    private List<DemoWindow> defaultAgenda() {
        List<DemoWindow> agenda = new ArrayList<>();
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            agenda.add(new DemoWindow(day, LocalTime.of(9, 0), LocalTime.of(13, 0)));
            agenda.add(new DemoWindow(day, LocalTime.of(14, 0), LocalTime.of(19, 0)));
        }
        return agenda;
    }

    private boolean isInsideAnyConfiguredWindow(Instant start, List<DemoWindow> agenda) {
        ZonedDateTime local = start.atZone(ZONE);
        LocalTime time = local.toLocalTime();
        return agenda.stream().anyMatch(window -> window.day() == local.getDayOfWeek()
            && !time.isBefore(window.start()) && time.isBefore(window.end()));
    }

    private LocalDate localDate(Instant instant) {
        return instant.atZone(ZONE).toLocalDate();
    }

    private boolean overlaps(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }
}
