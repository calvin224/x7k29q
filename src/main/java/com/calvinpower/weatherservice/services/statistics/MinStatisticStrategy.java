package com.calvinpower.weatherservice.services.statistics;

import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.repository.MeasurementRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Component
public class MinStatisticStrategy implements StatisticStrategy {

    private final MeasurementRepository measurementRepository;

    public MinStatisticStrategy(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    @Override
    public Statistic getStatistic() {
        return Statistic.MIN;
    }

    @Override
    public List<MeasurementAggregate> calculate(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Instant from,
            Instant to
    ) {
        return measurementRepository
                .findMinBySensorsAndMetricsAndRecordedAtBetween(
                        sensorIds,
                        metrics,
                        from,
                        to
                );
    }
}