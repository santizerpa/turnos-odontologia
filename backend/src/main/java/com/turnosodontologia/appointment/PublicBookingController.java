package com.turnosodontologia.appointment;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/public")
public class PublicBookingController {
    private final PublicBookingService bookings;

    public PublicBookingController(PublicBookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/slots")
    public PublicSlotsResponse slots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookings.slots(date);
    }

    @PostMapping("/appointments")
    public ResponseEntity<PublicBookingResponse> book(@Valid @RequestBody PublicBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookings.book(request));
    }
}
