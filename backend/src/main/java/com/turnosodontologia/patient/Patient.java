package com.turnosodontologia.patient;

import com.turnosodontologia.common.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(name = "patients", indexes = @Index(name = "idx_patient_dni", columnList = "dni"))
public class Patient extends AuditableEntity {
    private static final Pattern DNI = Pattern.compile("\\d{7,8}");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 255)
    private String email;

    protected Patient() {}

    public Patient(String dni, String firstName, String lastName, String email) {
        this.dni = normalizeDni(dni);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = normalizeEmail(email);
    }

    public void updateProfile(String firstName, String lastName, String email) {
        String normalizedEmail = normalizeEmail(email);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = normalizedEmail;
    }

    public static String normalizeDni(String dni) {
        if (dni == null) throw new IllegalArgumentException("DNI requerido");
        String normalized = dni.replace(".", "").replace(" ", "");
        if (!DNI.matcher(normalized).matches()) throw new IllegalArgumentException("DNI inválido");
        return normalized;
    }

    public static String normalizeEmail(String email) {
        if (email == null) throw new IllegalArgumentException("Email requerido");
        String normalized = email.trim().toLowerCase();
        if (!EMAIL.matcher(normalized).matches()) throw new IllegalArgumentException("Email inválido");
        return normalized;
    }

    public UUID getId() { return id; }
    public String getDni() { return dni; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
}
