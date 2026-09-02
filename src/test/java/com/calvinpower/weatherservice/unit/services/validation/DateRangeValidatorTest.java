package com.calvinpower.weatherservice.unit.services.validation;

import com.calvinpower.weatherservice.services.validation.DateRangeValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateRangeValidatorTest {

    private final DateRangeValidator validator =
            new DateRangeValidator();

    @Test
    void given_no_dates_when_validating_then_does_not_throw() {
        assertDoesNotThrow(() ->
                validator.validate(null, null)
        );
    }

    @Test
    void given_valid_date_range_when_validating_then_does_not_throw() {
        Instant from =
                Instant.parse("2026-09-01T00:00:00Z");
        Instant to =
                Instant.parse("2026-09-07T00:00:00Z");

        assertDoesNotThrow(() ->
                validator.validate(from, to)
        );
    }

    @Test
    void given_only_from_when_validating_then_throws_exception() {
        Instant from =
                Instant.parse("2026-09-01T00:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(from, null)
        );
    }

    @Test
    void given_only_to_when_validating_then_throws_exception() {
        Instant to =
                Instant.parse("2026-09-07T00:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(null, to)
        );
    }

    @Test
    void given_same_from_and_to_when_validating_then_throws_exception() {
        Instant date =
                Instant.parse("2026-09-01T00:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(date, date)
        );
    }

    @Test
    void given_less_than_one_day_when_validating_then_throws_exception() {
        Instant from =
                Instant.parse("2026-09-01T00:00:00Z");
        Instant to =
                Instant.parse("2026-09-01T23:59:59Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(from, to)
        );
    }

    @Test
    void given_exactly_one_day_when_validating_then_does_not_throw() {
        Instant from =
                Instant.parse("2026-09-01T00:00:00Z");
        Instant to =
                Instant.parse("2026-09-02T00:00:00Z");

        assertDoesNotThrow(() ->
                validator.validate(from, to)
        );
    }

    @Test
    void given_exactly_one_calendar_month_when_validating_then_does_not_throw() {
        Instant from =
                Instant.parse("2026-01-15T00:00:00Z");
        Instant to =
                Instant.parse("2026-02-15T00:00:00Z");

        assertDoesNotThrow(() ->
                validator.validate(from, to)
        );
    }

    @Test
    void given_more_than_one_calendar_month_when_validating_then_throws_exception() {
        Instant from =
                Instant.parse("2026-01-15T00:00:00Z");
        Instant to =
                Instant.parse("2026-02-16T00:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(from, to)
        );
    }
}