package com.calvinpower.weatherservice.services;

import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.repository.MeasurementRepository;
import com.calvinpower.weatherservice.services.statistics.StatisticStrategy;
import com.calvinpower.weatherservice.services.statistics.StatisticStrategyFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
public class MeasurementServiceImpl implements MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final StatisticStrategyFactory statisticStrategyFactory;

    public MeasurementServiceImpl(
            MeasurementRepository measurementRepository,
            StatisticStrategyFactory statisticStrategyFactory
    ) {
        this.measurementRepository = measurementRepository;
        this.statisticStrategyFactory = statisticStrategyFactory;
    }

    @Override
    public List<Measurement> getMeasurements(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Instant from,
            Instant to
    ) {
        return measurementRepository
                .findBySensor_IdInAndMetricInAndRecordedAtBetween(
                        sensorIds,
                        metrics,
                        from,
                        to
                );
    }

    @Override
    public List<MeasurementAggregate> getStatistics(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Statistic statistic,
            Instant from,
            Instant to
    ) {
        StatisticStrategy strategy =
                statisticStrategyFactory.getStrategy(statistic);

        return strategy.calculate(
                sensorIds,
                metrics,
                from,
                to
        );
    }
}