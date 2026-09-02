package com.calvinpower.weatherservice.dto;

import com.calvinpower.weatherservice.model.Metric;

import java.time.Instant;

public record MeasurementQueryResponse(
        Long sensorId,
        Metric metric,
        Double value,
        Instant recordedAt
) {
}