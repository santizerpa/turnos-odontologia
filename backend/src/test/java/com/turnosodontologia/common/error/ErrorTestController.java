package com.turnosodontologia.common.error;

import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/error-tests")
class ErrorTestController {
    @GetMapping("/runtime")
    void runtime() {
        throw new IllegalStateException(
            "SQL-value-sentinel org.hibernate Exception at com.fake.Handler runtime-token-sentinel runtime-dni-sentinel",
            new SQLException("SQL-value-sentinel patient-DNI runtime-dni-sentinel token runtime-token-sentinel"));
    }

    @GetMapping("/status")
    void status() {
        throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT);
    }

    @PostMapping("/post-only")
    void postOnly() {
    }
}
