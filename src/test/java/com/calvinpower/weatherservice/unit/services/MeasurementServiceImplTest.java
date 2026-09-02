package com.calvinpower.weatherservice.unit.services;

import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.repository.MeasurementRepository;
import com.calvinpower.weatherservice.services.MeasurementServiceImpl;
import com.calvinpower.weatherservice.services.statistics.StatisticStrategy;
import com.calvinpower.weatherservice.services.statistics.StatisticStrategyFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MeasurementServiceImplTest {

    private final MeasurementRepository measurementRepository =
            mock(MeasurementRepository.class);

    private final StatisticStrategyFactory statisticStrategyFactory =
            mock(StatisticStrategyFactory.class);

    private final MeasurementServiceImpl measurementService =
            new MeasurementServiceImpl(
                    measurementRepository,
                    statisticStrategyFactory
            );

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