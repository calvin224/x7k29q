package com.calvinpower.weatherservice.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

    private static final String INVALID_REQUEST_TITLE = "Invalid request";

    @ExceptionHandler(SensorNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleSensorNotFound(
            SensorNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Sensor not found",
                exception.getMessage(),
                "SENSOR_NOT_FOUND",
                request
        );
    }

    @ExceptionHandler(DuplicateSensorNameException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateSensorName(
            DuplicateSensorNameException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Sensor already exists",
                exception.getMessage(),
                "DUPLICATE_SENSOR_NAME",
                request
        );
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDateRange(
            InvalidDateRangeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid date range",
                exception.getMessage(),
                "INVALID_DATE_RANGE",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationFailure(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        boolean invalidSensorSelection = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .anyMatch(message -> message.startsWith("allSensors,"));

        String detail = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String message = Objects.requireNonNullElse(
                            error.getDefaultMessage(),
                            "is invalid"
                    );

                    if (error instanceof FieldError fieldError
                            && !message.startsWith(fieldError.getField())) {
                        return fieldError.getField() + " " + message;
                    }

                    return message;
                })
                .collect(Collectors.joining("; "));

        return problem(
                HttpStatus.BAD_REQUEST,
                invalidSensorSelection
                        ? "Invalid sensor selection"
                        : INVALID_REQUEST_TITLE,
                detail,
                invalidSensorSelection
                        ? "INVALID_SENSOR_SELECTION"
                        : "VALIDATION_FAILED",
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableRequest(
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST_TITLE,
                "Request body is missing or contains invalid JSON",
                "VALIDATION_FAILED",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST_TITLE,
                exception.getMessage(),
                "VALIDATION_FAILED",
                request
        );
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            String code,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", code);

        return ResponseEntity
                .status(status)
                .body(problemDetail);
    }
}
