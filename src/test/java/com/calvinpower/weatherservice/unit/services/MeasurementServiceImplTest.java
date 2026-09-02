package com.calvinpower.weatherservice.unit.services;

import com.calvinpower.weatherservice.exception.SensorNotFoundException;
import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Sensor;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.repository.MeasurementRepository;
import com.calvinpower.weatherservice.repository.SensorRepository;
import com.calvinpower.weatherservice.services.MeasurementServiceImpl;
import com.calvinpower.weatherservice.services.statistics.StatisticStrategy;
import com.calvinpower.weatherservice.services.statistics.StatisticStrategyFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MeasurementServiceImplTest {

    private final MeasurementRepository measurementRepository =
            mock(MeasurementRepository.class);

    private final SensorRepository sensorRepository =
            mock(SensorRepository.class);

    private final StatisticStrategyFactory statisticStrategyFactory =
            mock(StatisticStrategyFactory.class);

    private final MeasurementServiceImpl measurementService =
            new MeasurementServiceImpl(
                    measurementRepository,
                    sensorRepository,
                    statisticStrategyFactory
            );

    @Test
    void given_sensor_exists_when_creating_measurement_then_saves_measurement() {
        Long sensorId = 1L;
        Sensor sensor = new Sensor(sensorId);
        Metric metric = Metric.TEMPERATURE;
        Double value = 21.5;
        Instant recordedAt = Instant.parse("2026-09-02T10:30:00Z");

        Measurement measurement = new Measurement(
                sensor,
                metric,
                value,
                recordedAt
        );

        when(sensorRepository.findById(sensorId))
                .thenReturn(Optional.of(sensor));

        when(measurementRepository.save(any(Measurement.class)))
                .thenReturn(measurement);

        Measurement result = measurementService.createMeasurement(
                sensorId,
                metric,
                value,
                recordedAt
        );

        assertEquals(measurement, result);

        verify(sensorRepository)
                .findById(sensorId);

        verify(measurementRepository)
                .save(any(Measurement.class));
    }

    @Test
    void given_sensor_does_not_exist_when_creating_measurement_then_throws_sensor_not_found_exception() {
        Long sensorId = 1L;
        Metric metric = Metric.TEMPERATURE;
        Double value = 21.5;
        Instant recordedAt = Instant.parse("2026-09-02T10:30:00Z");

        when(sensorRepository.findById(sensorId))
                .thenReturn(Optional.empty());

        assertThrows(
                SensorNotFoundException.class,
                () -> measurementService.createMeasurement(
                        sensorId,
                        metric,
                        value,
                        recordedAt
                )
        );

        verify(sensorRepository)
                .findById(sensorId);

        verify(measurementRepository, never())
                .save(any(Measurement.class));
    }

    @Test
    void given_measurement_query_when_getting_measurements_then_returns_repository_results() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T00:00:00Z");

        List<Long> sensorIds = List.of(1L);
        List<Metric> metrics = List.of(Metric.TEMPERATURE);

        when(measurementRepository
                .findBySensor_IdInAndMetricInAndRecordedAtBetween(
                        sensorIds,
                        metrics,
                        from,
                        to
                ))
                .thenReturn(List.of());

        var result = measurementService.getMeasurements(
                sensorIds,
                metrics,
                from,
                to
        );

        assertEquals(List.of(), result);

        verify(measurementRepository)
                .findBySensor_IdInAndMetricInAndRecordedAtBetween(
                        sensorIds,
                        metrics,
                        from,
                        to
                );
    }

    @Test
    void given_statistic_query_when_getting_statistics_then_uses_selected_strategy() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T00:00:00Z");

        List<Long> sensorIds = List.of(1L);
        List<Metric> metrics = List.of(Metric.TEMPERATURE);

        StatisticStrategy strategy = mock(StatisticStrategy.class);

        List<MeasurementAggregate> expected = List.of(
                new MeasurementAggregate(
                        1L,
                        Metric.TEMPERATURE,
                        21.5
                )
        );

        when(statisticStrategyFactory.getStrategy(Statistic.AVERAGE))
                .thenReturn(strategy);

        when(strategy.calculate(
                sensorIds,
                metrics,
                from,
                to
        )).thenReturn(expected);

        var result = measurementService.getStatistics(
                sensorIds,
                metrics,
                Statistic.AVERAGE,
                from,
                to
        );

        assertEquals(expected, result);

        verify(statisticStrategyFactory)
                .getStrategy(Statistic.AVERAGE);

        verify(strategy)
                .calculate(
                        sensorIds,
                        metrics,
                        from,
                        to
                );
    }
}