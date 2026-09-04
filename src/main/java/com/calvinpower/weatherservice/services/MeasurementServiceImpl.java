package com.calvinpower.weatherservice.services;

import com.calvinpower.weatherservice.exception.DuplicateMeasurementException;
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
import com.calvinpower.weatherservice.services.validation.DateRangeValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MeasurementServiceImpl implements MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final SensorRepository sensorRepository;
    private final StatisticStrategyFactory statisticStrategyFactory;
    private final DateRangeValidator dateRangeValidator;

    public MeasurementServiceImpl(
            MeasurementRepository measurementRepository,
            SensorRepository sensorRepository,
            StatisticStrategyFactory statisticStrategyFactory,
            DateRangeValidator dateRangeValidator
    ) {
        this.measurementRepository = measurementRepository;
        this.sensorRepository = sensorRepository;
        this.statisticStrategyFactory = statisticStrategyFactory;
        this.dateRangeValidator = dateRangeValidator;
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

        if (measurementRepository.existsBySensor_IdAndMetricAndRecordedAt(
                sensorId,
                metric,
                recordedAt
        )) {
            throw new DuplicateMeasurementException(metric);
        }

        Measurement measurement = new Measurement(
                sensor,
                metric,
                value,
                recordedAt
        );

        try {
            return measurementRepository.saveAndFlush(measurement);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateMeasurementException(metric);
        }
    }

    @Override
    public List<Measurement> getMeasurements(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Instant from,
            Instant to
    ) {
        dateRangeValidator.validate(from, to);
        validateSensorsExist(sensorIds);

        if (sensorIds.isEmpty()) {
            if (from == null && to == null) {
                return measurementRepository
                        .findLatestByMetrics(metrics);
            }

            return measurementRepository
                    .findByMetricsAndRecordedAtBetween(
                            metrics,
                            from,
                            to
                    );
        }

        if (from == null && to == null) {
            return measurementRepository
                    .findLatestBySensorIdsAndMetrics(
                            sensorIds,
                            metrics
                    );
        }

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
        dateRangeValidator.validate(from, to);
        validateSensorsExist(sensorIds);

        Instant effectiveFrom = from;
        Instant effectiveTo = to;

        if (from == null && to == null) {
            Optional<Instant> latestRecordedAt = sensorIds.isEmpty()
                    ? measurementRepository.findLatestRecordedAtByMetrics(metrics)
                    : measurementRepository.findLatestRecordedAtBySensorIdsAndMetrics(
                            sensorIds,
                            metrics
                    );

            if (latestRecordedAt.isEmpty()) {
                return List.of();
            }

            effectiveTo = latestRecordedAt.get();
            effectiveFrom = effectiveTo.minus(Duration.ofDays(1));
        }

        StatisticStrategy strategy =
                statisticStrategyFactory.getStrategy(statistic);

        return strategy.calculate(
                sensorIds,
                metrics,
                effectiveFrom,
                effectiveTo
        );
    }

    private void validateSensorsExist(Collection<Long> sensorIds) {
        if (sensorIds.isEmpty()) {
            return;
        }

        Set<Long> requestedSensorIds = new LinkedHashSet<>(sensorIds);
        Set<Long> existingSensorIds = new HashSet<>();

        sensorRepository.findAllById(requestedSensorIds)
                .forEach(sensor -> existingSensorIds.add(sensor.getId()));

        requestedSensorIds.stream()
                .filter(sensorId -> !existingSensorIds.contains(sensorId))
                .findFirst()
                .ifPresent(sensorId -> {
                    throw new SensorNotFoundException(sensorId);
                });
    }
}
