package com.turnosodontologia.seed;

import java.time.LocalDate;
import java.util.List;

/**
 * Fixtures reservados del conjunto de demo. Todos los datos son ficticios:
 * los DNIs usan el rango 99.000.000+ y los emails terminan en example.invalid.
 */
final class DemoFixtures {
    static final String BLOCK_REASON_PREFIX = "Demo seed ";
    static final String BLOCK_REASON_SUFFIX = " · Bloqueo de agenda ficticio";

    record DemoPatient(String dni, String firstName, String lastName, String email) {}

    static final List<DemoPatient> PATIENTS = List.of(
        new DemoPatient("99000101", "Lucía", "Ficticia", "demo.paciente1@example.invalid"),
        new DemoPatient("99000102", "Mateo", "Ficticio", "demo.paciente2@example.invalid"),
        new DemoPatient("99000103", "Sofía", "Ficticia", "demo.paciente3@example.invalid"),
        new DemoPatient("99000104", "Tomás", "Ficticio", "demo.paciente4@example.invalid"),
        new DemoPatient("99000105", "Valentina", "Ficticia", "demo.paciente5@example.invalid"),
        new DemoPatient("99000106", "Joaquín", "Ficticio", "demo.paciente6@example.invalid"),
        new DemoPatient("99000107", "Camila", "Ficticia", "demo.paciente7@example.invalid")
    );

    private DemoFixtures() {
    }

    static String blockReason(LocalDate effectiveDate) {
        return BLOCK_REASON_PREFIX + effectiveDate + BLOCK_REASON_SUFFIX;
    }

    static boolean isDemoReason(String reason) {
        return reason != null && reason.startsWith(BLOCK_REASON_PREFIX);
    }

    static LocalDate parseBlockDate(String reason) {
        if (!isDemoReason(reason) || reason.length() < BLOCK_REASON_PREFIX.length() + 10) {
            throw new DemoSeedException(
                "El bloqueo de demo existente no tiene una fecha efectiva legible. Prepará una base local de demo limpia.");
        }
        try {
            return LocalDate.parse(reason.substring(BLOCK_REASON_PREFIX.length(),
                BLOCK_REASON_PREFIX.length() + 10));
        } catch (RuntimeException exception) {
            throw new DemoSeedException(
                "El bloqueo de demo existente no tiene una fecha efectiva válida. Prepará una base local de demo limpia.",
                exception);
        }
    }
}
