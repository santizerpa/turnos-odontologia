package com.turnosodontologia.appointment;

public class PublicTokenExpiredException extends RuntimeException {
    public PublicTokenExpiredException() {
        super("Token vencido");
    }
}
