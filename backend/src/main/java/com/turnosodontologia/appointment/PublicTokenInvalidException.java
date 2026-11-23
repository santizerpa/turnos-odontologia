package com.turnosodontologia.appointment;

public class PublicTokenInvalidException extends RuntimeException {
    public PublicTokenInvalidException() {
        super("Token inexistente o inválido");
    }
}
