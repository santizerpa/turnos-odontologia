package com.turnosodontologia.appointment;

import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "appointment_tokens")
public class AppointmentToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppointmentToken() {}

    public AppointmentToken(Appointment appointment, String opaqueTokenHash, Instant expiresAt) {
        if (appointment == null || opaqueTokenHash == null || expiresAt == null) throw new IllegalArgumentException("Token requerido");
        this.appointment = appointment;
        this.tokenHash = opaqueTokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public static String sha256(String plainToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    @PrePersist
    void auditCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public Appointment getAppointment() { return appointment; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void replaceOpaqueTokenHashAndExpiry(String opaqueTokenHash, Instant expiresAt) {
        if (opaqueTokenHash == null || expiresAt == null) throw new IllegalArgumentException("Token requerido");
        this.tokenHash = opaqueTokenHash;
        this.expiresAt = expiresAt;
    }
}
