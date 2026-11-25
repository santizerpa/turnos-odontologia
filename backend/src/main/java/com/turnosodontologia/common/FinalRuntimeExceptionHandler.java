package com.turnosodontologia.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class FinalRuntimeExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(FinalRuntimeExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(RuntimeException exception) {
        if (exception instanceof ErrorResponse) {
            throw exception;
        }

        // Exception messages and causes may contain request data, tokens, or SQL values.
        log.error("Unhandled runtime exception type={} rootCauseType={}",
                exception.getClass().getName(), rootCause(exception).getClass().getName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno"));
    }

    private Throwable rootCause(Throwable exception) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable root = exception;
        visited.add(root);
        while (root.getCause() != null && visited.add(root.getCause())) {
            root = root.getCause();
        }
        return root;
    }
}
