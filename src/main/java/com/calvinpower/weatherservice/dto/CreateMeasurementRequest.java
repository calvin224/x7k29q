package com.calvinpower.weatherservice.dto;

import com.calvinpower.weatherservice.model.Metric;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateMeasurementRequest(

        @NotNull
        Metric metric,

        @NotNull
        Double value,

        @NotNull
        Instant recordedAt

) {
}