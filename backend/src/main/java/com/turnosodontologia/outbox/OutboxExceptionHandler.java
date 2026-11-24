package com.turnosodontologia.outbox;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OutboxController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OutboxExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail unexpected(RuntimeException ignored) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error interno al procesar la bandeja"
        );
        problem.setTitle("Error interno");
        return problem;
    }
}
