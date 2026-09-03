package com.calvinpower.weatherservice.dto;

import com.calvinpower.weatherservice.model.Metric;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateMeasurementRequest(

        @Schema(
                description = "Type of weather reading.",
                example = "TEMPERATURE"
        )
        @NotNull
        Metric metric,

        @Schema(
                description = "Numeric reading for the selected metric.",
                example = "21.5"
        )
        @NotNull
        Double value,

        @Schema(
                description = "UTC timestamp when the reading was captured.",
                example = "2026-09-02T10:30:00Z",
                format = "date-time"
        )
        @NotNull
        Instant recordedAt

) {
}
