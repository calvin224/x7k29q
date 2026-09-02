package com.calvinpower.weatherservice.dto;

import com.calvinpower.weatherservice.model.Metric;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record MeasurementQuery(
        @NotNull
        List<Long> sensorIds,

        @NotEmpty
        List<Metric> metrics,

        Instant from,
        Instant to
) {
}