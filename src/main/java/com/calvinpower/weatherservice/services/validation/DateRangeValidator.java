package com.calvinpower.weatherservice.services.validation;

import com.calvinpower.weatherservice.exception.InvalidDateRangeException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

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

    public static final class SensorSelectionRules {

        public static final String ALL_SENSORS_MESSAGE =
                "When allSensors is true, sensorIds and sensorNames must be empty";
        public static final String SPECIFIC_SENSORS_MESSAGE =
                "When allSensors is false, at least one sensor ID or sensor name must be provided";

        private SensorSelectionRules() {
        }

        public static boolean isAllSensorsSelectionValid(
                Boolean allSensors,
                List<Long> sensorIds,
                List<String> sensorNames
        ) {
            return cannotEvaluate(allSensors, sensorIds)
                    || !allSensors
                    || !hasSpecificSensors(sensorIds, sensorNames);
        }

        public static boolean isSpecificSensorsSelectionValid(
                Boolean allSensors,
                List<Long> sensorIds,
                List<String> sensorNames
        ) {
            return cannotEvaluate(allSensors, sensorIds)
                    || allSensors
                    || hasSpecificSensors(sensorIds, sensorNames);
        }

        private static boolean cannotEvaluate(Boolean allSensors, List<Long> sensorIds) {
            return allSensors == null || sensorIds == null;
        }

        private static boolean hasSpecificSensors(List<Long> sensorIds, List<String> sensorNames) {
            return !sensorIds.isEmpty() || sensorNames != null && !sensorNames.isEmpty();
        }
    }
}
