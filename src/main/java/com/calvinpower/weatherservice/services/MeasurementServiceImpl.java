package com.calvinpower.weatherservice.services;

import com.calvinpower.weatherservice.exception.SensorNotFoundException;
import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Sensor;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.repository.MeasurementRepository;
import com.calvinpower.weatherservice.repository.SensorRepository;
import com.calvinpower.weatherservice.services.statistics.StatisticStrategy;
import com.calvinpower.weatherservice.services.statistics.StatisticStrategyFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
public class MeasurementServiceImpl implements MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final SensorRepository sensorRepository;
    private final StatisticStrategyFactory statisticStrategyFactory;

    public MeasurementServiceImpl(
            MeasurementRepository measurementRepository,
            SensorRepository sensorRepository,
            StatisticStrategyFactory statisticStrategyFactory
    ) {
        this.measurementRepository = measurementRepository;
        this.sensorRepository = sensorRepository;
        this.statisticStrategyFactory = statisticStrategyFactory;
    }

    @Override
    public Measurement createMeasurement(
            Long sensorId,
            Metric metric,
            Double value,
            Instant recordedAt
    ) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() ->
                        new SensorNotFoundException(sensorId)
                );

        Measurement measurement = new Measurement(
                sensor,
                metric,
                value,
                recordedAt
        );

        return measurementRepository.save(measurement);
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