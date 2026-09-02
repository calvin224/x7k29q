package com.calvinpower.weatherservice.services;

import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface MeasurementService {

    Measurement createMeasurement(
            Long sensorId,
            Metric metric,
            Double value,
            Instant recordedAt
    );

    List<Measurement> getMeasurements(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Instant from,
            Instant to
    );

    List<MeasurementAggregate> getStatistics(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Statistic statistic,
            Instant from,
            Instant to
    );
}