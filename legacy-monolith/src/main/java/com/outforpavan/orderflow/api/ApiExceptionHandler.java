package com.outforpavan.orderflow.api;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Comparator;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException exception, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Object> handleInsufficientStock(InsufficientStockException exception,
                                                        WebRequest request) {
        return problem(HttpStatus.CONFLICT, "Insufficient stock", exception.getMessage(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                 HttpHeaders headers, HttpStatusCode status,
                                                                 WebRequest request) {
        ProblemDetail body = body(HttpStatus.BAD_REQUEST, "Validation failed",
                "Correct the invalid request fields and try again", request);
        body.setProperty("fieldErrors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
                .toList());
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                 HttpHeaders headers, HttpStatusCode status,
                                                                 WebRequest request) {
        return handleExceptionInternal(exception, body(HttpStatus.BAD_REQUEST, "Malformed request",
                "Request body must be valid JSON with the expected field types", request), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
                                                       HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, body(HttpStatus.BAD_REQUEST, "Invalid request parameter",
                "A request parameter has an invalid type", request), headers, status, request);
    }

    private ResponseEntity<Object> problem(HttpStatus status, String title, String detail, WebRequest request) {
        return ResponseEntity.status(status).body(body(status, title, detail, request));
    }

    private ProblemDetail body(HttpStatus status, String title, String detail, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        return problem;
    }

    public record FieldError(String field, String message) {
    }
}
