package com.calvinpower.weatherservice.repository;

import com.calvinpower.weatherservice.model.Metric;

public record MeasurementAggregate(
        Long sensorId,
        Metric metric,
        Double value
) {
}