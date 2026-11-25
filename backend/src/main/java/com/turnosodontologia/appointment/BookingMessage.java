package com.turnosodontologia.appointment;

/**
 * Subject and body of the booking e-mail that lands in the simulated outbox. Shared by the public booking
 * flow and the demo seed so both produce the same message, with working confirm and cancel links.
 */
public final class BookingMessage {
    public static final String SUBJECT = "Confirma tu turno";

    private BookingMessage() {}

    public static String body(String plainToken) {
        return "Para confirmar tu turno, visita /api/public/appointments/" + plainToken + "/confirm. "
                + "Para cancelarlo, visita /api/public/appointments/" + plainToken + "/cancel.";
    }
}
