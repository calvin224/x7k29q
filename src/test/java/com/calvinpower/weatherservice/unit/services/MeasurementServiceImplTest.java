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
import com.calvinpower.weatherservice.services.validation.DateRangeValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
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

    private final DateRangeValidator dateRangeValidator =
            new DateRangeValidator();

    private final MeasurementServiceImpl measurementService =
            new MeasurementServiceImpl(
                    measurementRepository,
                    sensorRepository,
                    statisticStrategyFactory,
                    dateRangeValidator
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
    void given_existing_sensor_when_getting_measurements_with_no_matching_data_then_returns_empty_result() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T00:00:00Z");

        List<Long> sensorIds = List.of(1L);
        List<Metric> metrics = List.of(Metric.TEMPERATURE);

        when(sensorRepository.findAllById(new LinkedHashSet<>(sensorIds)))
                .thenReturn(List.of(new Sensor(1L)));

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
    void given_missing_sensor_when_getting_measurements_then_throws_sensor_not_found_exception() {
        List<Long> sensorIds = List.of(999L);

        when(sensorRepository.findAllById(new LinkedHashSet<>(sensorIds)))
                .thenReturn(List.of());

        assertThrows(
                SensorNotFoundException.class,
                () -> measurementService.getMeasurements(
                        sensorIds,
                        List.of(Metric.TEMPERATURE),
                        Instant.parse("2026-09-01T00:00:00Z"),
                        Instant.parse("2026-09-02T00:00:00Z")
                )
        );

        verifyNoInteractions(measurementRepository);
    }

    @Test
    void given_existing_and_missing_sensor_ids_when_getting_measurements_then_throws_sensor_not_found_exception() {
        List<Long> sensorIds = List.of(1L, 999L);

        when(sensorRepository.findAllById(new LinkedHashSet<>(sensorIds)))
                .thenReturn(List.of(new Sensor(1L)));

        assertThrows(
                SensorNotFoundException.class,
                () -> measurementService.getMeasurements(
                        sensorIds,
                        List.of(Metric.TEMPERATURE),
                        Instant.parse("2026-09-01T00:00:00Z"),
                        Instant.parse("2026-09-02T00:00:00Z")
                )
        );

        verifyNoInteractions(measurementRepository);
    }

    @Test
    void given_duplicate_existing_sensor_ids_when_getting_measurements_then_does_not_report_missing_sensor() {
        List<Long> sensorIds = List.of(1L, 1L);
        List<Metric> metrics = List.of(Metric.TEMPERATURE);
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T00:00:00Z");

        when(sensorRepository.findAllById(new LinkedHashSet<>(sensorIds)))
                .thenReturn(List.of(new Sensor(1L)));
        when(measurementRepository
                .findBySensor_IdInAndMetricInAndRecordedAtBetween(
                        sensorIds,
                        metrics,
                        from,
                        to
                )).thenReturn(List.of());

        List<Measurement> result = measurementService.getMeasurements(
                sensorIds,
                metrics,
                from,
                to
        );

        assertEquals(List.of(), result);
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

        when(sensorRepository.findAllById(new LinkedHashSet<>(sensorIds)))
                .thenReturn(List.of(new Sensor(1L)));

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

    @Test
    void given_missing_sensor_when_getting_statistics_then_throws_sensor_not_found_exception() {
        List<Long> sensorIds = List.of(999L);

        when(sensorRepository.findAllById(new LinkedHashSet<>(sensorIds)))
                .thenReturn(List.of());

        assertThrows(
                SensorNotFoundException.class,
                () -> measurementService.getStatistics(
                        sensorIds,
                        List.of(Metric.TEMPERATURE),
                        Statistic.AVERAGE,
                        Instant.parse("2026-09-01T00:00:00Z"),
                        Instant.parse("2026-09-02T00:00:00Z")
                )
        );

        verifyNoInteractions(measurementRepository);
        verifyNoInteractions(statisticStrategyFactory);
    }

    @Test
    void given_no_dates_for_specific_sensors_when_getting_statistics_then_uses_latest_one_day_range() {
        List<Long> sensorIds = List.of(1L, 2L);
        List<Metric> metrics = List.of(Metric.TEMPERATURE);
        Instant latestRecordedAt = Instant.parse("2026-09-02T12:00:00Z");
        Instant expectedFrom = Instant.parse("2026-09-01T12:00:00Z");
        StatisticStrategy strategy = mock(StatisticStrategy.class);

        List<MeasurementAggregate> expected = List.of(
                new MeasurementAggregate(
                        1L,
                        Metric.TEMPERATURE,
                        25.0
                )
        );

        when(sensorRepository.findAllById(new LinkedHashSet<>(sensorIds)))
                .thenReturn(List.of(new Sensor(1L), new Sensor(2L)));

        when(measurementRepository.findLatestRecordedAtBySensorIdsAndMetrics(
                sensorIds,
                metrics
        )).thenReturn(Optional.of(latestRecordedAt));

        when(statisticStrategyFactory.getStrategy(Statistic.AVERAGE))
                .thenReturn(strategy);

        when(strategy.calculate(
                sensorIds,
                metrics,
                expectedFrom,
                latestRecordedAt
        )).thenReturn(expected);

        List<MeasurementAggregate> result = measurementService.getStatistics(
                sensorIds,
                metrics,
                Statistic.AVERAGE,
                null,
                null
        );

        assertEquals(expected, result);

        verify(measurementRepository)
                .findLatestRecordedAtBySensorIdsAndMetrics(
                        sensorIds,
                        metrics
                );

        verify(strategy).calculate(
                sensorIds,
                metrics,
                expectedFrom,
                latestRecordedAt
        );
    }

    @Test
    void given_no_dates_for_all_sensors_when_getting_statistics_then_uses_latest_one_day_range() {
        List<Long> sensorIds = List.of();
        List<Metric> metrics = List.of(Metric.TEMPERATURE, Metric.HUMIDITY);
        Instant latestRecordedAt = Instant.parse("2026-09-02T12:00:00Z");
        Instant expectedFrom = Instant.parse("2026-09-01T12:00:00Z");
        StatisticStrategy strategy = mock(StatisticStrategy.class);

        when(measurementRepository.findLatestRecordedAtByMetrics(metrics))
                .thenReturn(Optional.of(latestRecordedAt));

        when(statisticStrategyFactory.getStrategy(Statistic.MAX))
                .thenReturn(strategy);

        when(strategy.calculate(
                sensorIds,
                metrics,
                expectedFrom,
                latestRecordedAt
        )).thenReturn(List.of());

        List<MeasurementAggregate> result = measurementService.getStatistics(
                sensorIds,
                metrics,
                Statistic.MAX,
                null,
                null
        );

        assertEquals(List.of(), result);

        verify(measurementRepository)
                .findLatestRecordedAtByMetrics(metrics);

        verify(strategy).calculate(
                sensorIds,
                metrics,
                expectedFrom,
                latestRecordedAt
        );
    }

    @Test
    void given_no_matching_measurements_when_getting_latest_statistics_then_returns_empty_result() {
        List<Long> sensorIds = List.of(1L);
        List<Metric> metrics = List.of(Metric.TEMPERATURE);

        when(sensorRepository.findAllById(new LinkedHashSet<>(sensorIds)))
                .thenReturn(List.of(new Sensor(1L)));

        when(measurementRepository.findLatestRecordedAtBySensorIdsAndMetrics(
                sensorIds,
                metrics
        )).thenReturn(Optional.empty());

        List<MeasurementAggregate> result = measurementService.getStatistics(
                sensorIds,
                metrics,
                Statistic.SUM,
                null,
                null
        );

        assertEquals(List.of(), result);

        verify(measurementRepository)
                .findLatestRecordedAtBySensorIdsAndMetrics(
                        sensorIds,
                        metrics
                );

        verifyNoInteractions(statisticStrategyFactory);
    }

    @Test
    void given_invalid_measurement_date_range_when_getting_measurements_then_throws_exception() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-01T12:00:00Z");

        List<Long> sensorIds = List.of(1L);
        List<Metric> metrics = List.of(Metric.TEMPERATURE);

        assertThrows(
                IllegalArgumentException.class,
                () -> measurementService.getMeasurements(
                        sensorIds,
                        metrics,
                        from,
                        to
                )
        );

        verify(measurementRepository, never())
                .findBySensor_IdInAndMetricInAndRecordedAtBetween(
                        anyCollection(),
                        anyCollection(),
                        any(Instant.class),
                        any(Instant.class)
                );
    }

    @Test
    void given_invalid_statistic_date_range_when_getting_statistics_then_throws_exception() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-01T12:00:00Z");

        List<Long> sensorIds = List.of(1L);
        List<Metric> metrics = List.of(Metric.TEMPERATURE);

        assertThrows(
                IllegalArgumentException.class,
                () -> measurementService.getStatistics(
                        sensorIds,
                        metrics,
                        Statistic.AVERAGE,
                        from,
                        to
                )
        );

        verifyNoInteractions(statisticStrategyFactory);
    }

    @Test
    void given_no_dates_when_getting_measurements_then_returns_latest_measurements() {
        List<Long> sensorIds = List.of(1L);
        List<Metric> metrics = List.of(Metric.TEMPERATURE);

        Sensor sensor = new Sensor(1L);

        Measurement latestMeasurement = new Measurement(
                sensor,
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-02T10:30:00Z")
        );

        List<Measurement> expected = List.of(latestMeasurement);

        when(sensorRepository.findAllById(new LinkedHashSet<>(sensorIds)))
                .thenReturn(List.of(sensor));

        when(measurementRepository.findLatestBySensorIdsAndMetrics(
                sensorIds,
                metrics
        )).thenReturn(expected);

        List<Measurement> result =
                measurementService.getMeasurements(
                        sensorIds,
                        metrics,
                        null,
                        null
                );

        assertEquals(expected, result);

        verify(measurementRepository)
                .findLatestBySensorIdsAndMetrics(
                        sensorIds,
                        metrics
                );

        verify(measurementRepository, never())
                .findBySensor_IdInAndMetricInAndRecordedAtBetween(
                        anyCollection(),
                        anyCollection(),
                        any(Instant.class),
                        any(Instant.class)
                );
    }

    @Test
    void given_all_sensors_when_getting_measurements_then_does_not_validate_specific_sensor_ids() {
        List<Long> sensorIds = List.of();
        List<Metric> metrics = List.of(Metric.TEMPERATURE);

        Instant from =
                Instant.parse("2026-09-01T00:00:00Z");
        Instant to =
                Instant.parse("2026-09-02T00:00:00Z");

        Sensor firstSensor = new Sensor(1L);
        Sensor secondSensor = new Sensor(2L);

        List<Measurement> expected = List.of(
                new Measurement(
                        firstSensor,
                        Metric.TEMPERATURE,
                        21.5,
                        Instant.parse("2026-09-01T10:00:00Z")
                ),
                new Measurement(
                        secondSensor,
                        Metric.TEMPERATURE,
                        25.0,
                        Instant.parse("2026-09-01T11:00:00Z")
                )
        );

        when(measurementRepository.findByMetricsAndRecordedAtBetween(
                metrics,
                from,
                to
        )).thenReturn(expected);

        List<Measurement> result =
                measurementService.getMeasurements(
                        sensorIds,
                        metrics,
                        from,
                        to
                );

        assertEquals(expected, result);

        verify(sensorRepository, never())
                .findAllById(anyCollection());

        verify(measurementRepository)
                .findByMetricsAndRecordedAtBetween(
                        metrics,
                        from,
                        to
                );

        verify(measurementRepository, never())
                .findBySensor_IdInAndMetricInAndRecordedAtBetween(
                        anyCollection(),
                        anyCollection(),
                        any(Instant.class),
                        any(Instant.class)
                );
    }


}
