package com.turnosodontologia.auth.controller;

import com.turnosodontologia.auth.dto.AuthErrorResponse;
import com.turnosodontologia.auth.dto.DentistProfileResponse;
import com.turnosodontologia.auth.dto.LoginRequest;
import com.turnosodontologia.auth.principal.DentistPrincipal;
import com.turnosodontologia.auth.service.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String GENERIC_ERROR = "Credenciales inválidas";

    private final AuthSessionService sessionService;

    public AuthController(AuthSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@RequestBody(required = false) LoginRequest request,
                            HttpServletRequest httpRequest,
                            HttpServletResponse httpResponse) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            return unauthorized();
        }
        try {
            String email = request.email();
            Authentication authentication = sessionService.login(email, request.password());
            sessionService.establishSession(authentication, httpRequest, httpResponse);
            return ResponseEntity.ok(DentistProfileResponse.from((DentistPrincipal) authentication.getPrincipal()));
        } catch (AuthenticationException exception) {
            return unauthorized();
        }
    }

    @GetMapping("/me")
    ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DentistPrincipal principal)) {
            return unauthorized();
        }
        return ResponseEntity.ok(DentistProfileResponse.from(principal));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        sessionService.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthErrorResponse> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthErrorResponse(GENERIC_ERROR));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
