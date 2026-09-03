package com.calvinpower.weatherservice.dto;

import com.calvinpower.weatherservice.model.Metric;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record MeasurementResponse(
        @Schema(description = "Generated measurement ID.", example = "42")
        Long id,

        @Schema(description = "ID of the sensor that produced the reading.", example = "1")
        Long sensorId,

        @Schema(description = "Type of weather reading.", example = "TEMPERATURE")
        Metric metric,

        @Schema(description = "Recorded numeric value.", example = "21.5")
        Double value,

        @Schema(
                description = "UTC timestamp when the reading was captured.",
                example = "2026-09-02T10:30:00Z",
                format = "date-time"
        )
        Instant recordedAt
) {
}
