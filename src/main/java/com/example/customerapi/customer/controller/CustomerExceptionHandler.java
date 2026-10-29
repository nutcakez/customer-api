package com.example.customerapi.customer.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.customerapi.customer.service.CustomerNotFoundException;
import com.example.customerapi.customer.service.EmailAlreadyExistsException;

/**
 * Maps the customer service's exceptions to HTTP responses (RFC 9457 problem details).
 * AccessDeniedException is deliberately not handled here, Spring Security turns it into a 403.
 * Ordered first so the validation handler wins over Spring Boot's generic problem details handler.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CustomerController.class)
public class CustomerExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleNotFound(CustomerNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        List<Map<String, String>> errors = e.getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body is invalid");
        problem.setProperty("errors", errors);
        return problem;
    }
}
