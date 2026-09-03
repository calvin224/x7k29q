package com.calvinpower.weatherservice.services.validation;

import com.calvinpower.weatherservice.exception.InvalidDateRangeException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class DateRangeValidator {

    public void validate(Instant from, Instant to) {
        if (from == null && to == null) {
            return;
        }

        if (from == null || to == null) {
            throw new InvalidDateRangeException(
                    "Both from and to must be provided together"
            );
        }

        if (!from.isBefore(to)) {
            throw new InvalidDateRangeException(
                    "from must be before to"
            );
        }

        if (to.isBefore(from.plusSeconds(86_400))) {
            throw new InvalidDateRangeException(
                    "Date range must be at least one day"
            );
        }

        OffsetDateTime fromDateTime =
                from.atOffset(ZoneOffset.UTC);

        OffsetDateTime toDateTime =
                to.atOffset(ZoneOffset.UTC);

        if (toDateTime.isAfter(fromDateTime.plusMonths(1))) {
            throw new InvalidDateRangeException(
                    "Date range must not exceed one month"
            );
        }
    }
}
