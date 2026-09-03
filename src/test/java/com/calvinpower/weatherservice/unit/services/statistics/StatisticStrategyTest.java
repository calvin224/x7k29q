package com.calvinpower.weatherservice.unit.services.statistics;

import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.repository.MeasurementRepository;
import com.calvinpower.weatherservice.services.statistics.AverageStatisticStrategy;
import com.calvinpower.weatherservice.services.statistics.MaxStatisticStrategy;
import com.calvinpower.weatherservice.services.statistics.MinStatisticStrategy;
import com.calvinpower.weatherservice.services.statistics.SumStatisticStrategy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatisticStrategyTest {

    private static final List<Long> SENSOR_IDS = List.of(1L);
    private static final List<Metric> METRICS = List.of(Metric.TEMPERATURE);
    private static final Instant FROM =
            Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO =
            Instant.parse("2026-09-02T00:00:00Z");
    private static final List<MeasurementAggregate> AGGREGATES = List.of(
            new MeasurementAggregate(1L, Metric.TEMPERATURE, 21.5)
    );

    @Test
    void given_average_strategy_when_calculating_statistics_then_uses_matching_repository_queries() {
        MeasurementRepository repository = mock(MeasurementRepository.class);
        AverageStatisticStrategy strategy =
                new AverageStatisticStrategy(repository);

        when(repository.findAverageBySensorsAndMetricsAndRecordedAtBetween(
                SENSOR_IDS,
                METRICS,
                FROM,
                TO
        )).thenReturn(AGGREGATES);
        when(repository.findAverageByMetricsAndRecordedAtBetween(
                METRICS,
                FROM,
                TO
        )).thenReturn(AGGREGATES);

        assertEquals(Statistic.AVERAGE, strategy.getStatistic());
        assertEquals(
                AGGREGATES,
                strategy.calculate(SENSOR_IDS, METRICS, FROM, TO)
        );
        assertEquals(
                AGGREGATES,
                strategy.calculate(List.of(), METRICS, FROM, TO)
        );

        verify(repository).findAverageBySensorsAndMetricsAndRecordedAtBetween(
                SENSOR_IDS,
                METRICS,
                FROM,
                TO
        );
        verify(repository).findAverageByMetricsAndRecordedAtBetween(
                METRICS,
                FROM,
                TO
        );
    }

    @Test
    void given_min_strategy_when_calculating_statistics_then_uses_matching_repository_queries() {
        MeasurementRepository repository = mock(MeasurementRepository.class);
        MinStatisticStrategy strategy = new MinStatisticStrategy(repository);

        when(repository.findMinBySensorsAndMetricsAndRecordedAtBetween(
                SENSOR_IDS,
                METRICS,
                FROM,
                TO
        )).thenReturn(AGGREGATES);
        when(repository.findMinByMetricsAndRecordedAtBetween(
                METRICS,
                FROM,
                TO
        )).thenReturn(AGGREGATES);

        assertEquals(Statistic.MIN, strategy.getStatistic());
        assertEquals(
                AGGREGATES,
                strategy.calculate(SENSOR_IDS, METRICS, FROM, TO)
        );
        assertEquals(
                AGGREGATES,
                strategy.calculate(List.of(), METRICS, FROM, TO)
        );

        verify(repository).findMinBySensorsAndMetricsAndRecordedAtBetween(
                SENSOR_IDS,
                METRICS,
                FROM,
                TO
        );
        verify(repository).findMinByMetricsAndRecordedAtBetween(
                METRICS,
                FROM,
                TO
        );
    }

    @Test
    void given_max_strategy_when_calculating_statistics_then_uses_matching_repository_queries() {
        MeasurementRepository repository = mock(MeasurementRepository.class);
        MaxStatisticStrategy strategy = new MaxStatisticStrategy(repository);

        when(repository.findMaxBySensorsAndMetricsAndRecordedAtBetween(
                SENSOR_IDS,
                METRICS,
                FROM,
                TO
        )).thenReturn(AGGREGATES);
        when(repository.findMaxByMetricsAndRecordedAtBetween(
                METRICS,
                FROM,
                TO
        )).thenReturn(AGGREGATES);

        assertEquals(Statistic.MAX, strategy.getStatistic());
        assertEquals(
                AGGREGATES,
                strategy.calculate(SENSOR_IDS, METRICS, FROM, TO)
        );
        assertEquals(
                AGGREGATES,
                strategy.calculate(List.of(), METRICS, FROM, TO)
        );

        verify(repository).findMaxBySensorsAndMetricsAndRecordedAtBetween(
                SENSOR_IDS,
                METRICS,
                FROM,
                TO
        );
        verify(repository).findMaxByMetricsAndRecordedAtBetween(
                METRICS,
                FROM,
                TO
        );
    }

    @Test
    void given_sum_strategy_when_calculating_statistics_then_uses_matching_repository_queries() {
        MeasurementRepository repository = mock(MeasurementRepository.class);
        SumStatisticStrategy strategy = new SumStatisticStrategy(repository);

        when(repository.findSumBySensorsAndMetricsAndRecordedAtBetween(
                SENSOR_IDS,
                METRICS,
                FROM,
                TO
        )).thenReturn(AGGREGATES);
        when(repository.findSumByMetricsAndRecordedAtBetween(
                METRICS,
                FROM,
                TO
        )).thenReturn(AGGREGATES);

        assertEquals(Statistic.SUM, strategy.getStatistic());
        assertEquals(
                AGGREGATES,
                strategy.calculate(SENSOR_IDS, METRICS, FROM, TO)
        );
        assertEquals(
                AGGREGATES,
                strategy.calculate(List.of(), METRICS, FROM, TO)
        );

        verify(repository).findSumBySensorsAndMetricsAndRecordedAtBetween(
                SENSOR_IDS,
                METRICS,
                FROM,
                TO
        );
        verify(repository).findSumByMetricsAndRecordedAtBetween(
                METRICS,
                FROM,
                TO
        );
    }
}
