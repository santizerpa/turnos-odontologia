package com.turnosodontologia.auth.dto;

import com.turnosodontologia.auth.principal.DentistPrincipal;

import java.util.UUID;

public record DentistProfileResponse(UUID id, String email, String firstName, String lastName) {

    public static DentistProfileResponse from(DentistPrincipal principal) {
        return new DentistProfileResponse(
            principal.id(),
            principal.email(),
            principal.firstName(),
            principal.lastName()
        );
    }
}
