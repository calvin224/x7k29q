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
public class AverageStatisticStrategy implements StatisticStrategy {

    private final MeasurementRepository measurementRepository;

    public AverageStatisticStrategy(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    @Override
    public Statistic getStatistic() {
        return Statistic.AVERAGE;
    }

    @Override
    public List<MeasurementAggregate> calculate(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Instant from,
            Instant to
    ) {
        return measurementRepository
                .findAverageBySensorsAndMetricsAndRecordedAtBetween(
                        sensorIds,
                        metrics,
                        from,
                        to
                );
    }
}