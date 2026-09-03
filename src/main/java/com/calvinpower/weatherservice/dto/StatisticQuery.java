package com.calvinpower.weatherservice.dto;

import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record StatisticQuery(
        @Schema(
                description = "Set to true to include every sensor; sensorIds and sensorNames must then be empty.",
                example = "false"
        )
        @NotNull
        Boolean allSensors,

        @Schema(
                description = "Sensor IDs to include. When allSensors is false, provide sensorIds, sensorNames, or both.",
                example = "[1]"
        )
        @NotNull
        List<Long> sensorIds,

        @Schema(
                description = "Exact sensor names to include. May be combined with sensorIds; use an empty array when selecting all sensors.",
                example = "[\"Dublin City Sensor\"]"
        )
        List<@NotBlank @Size(max = 100) String> sensorNames,

        @Schema(
                description = "One or more metric types to aggregate.",
                example = "[\"TEMPERATURE\"]"
        )
        @NotEmpty
        List<Metric> metrics,

        @Schema(
                description = "Aggregation to calculate for each sensor and metric.",
                example = "AVERAGE"
        )
        @NotNull
        Statistic statistic,

        @Schema(
                description = "Inclusive range start. Omit together with to to use the latest one-day period of matching data.",
                example = "2026-09-01T00:00:00Z",
                format = "date-time",
                nullable = true
        )
        Instant from,

        @Schema(
                description = "Inclusive range end. Omit together with from to use the latest one-day period of matching data.",
                example = "2026-09-02T00:00:00Z",
                format = "date-time",
                nullable = true
        )
        Instant to
) {

    @AssertTrue(message = "allSensors, sensorIds, and sensorNames must define a valid sensor selection")
    @Schema(hidden = true)
    public boolean isSensorSelectionValid() {
        if (allSensors == null || sensorIds == null) {
            return true;
        }

        boolean hasSensorNames =
                sensorNames != null && !sensorNames.isEmpty();

        return allSensors
                ? sensorIds.isEmpty() && !hasSensorNames
                : !sensorIds.isEmpty() || hasSensorNames;
    }
}
