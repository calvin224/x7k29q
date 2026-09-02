package com.calvinpower.weatherservice.services.statistics;

import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface StatisticStrategy {

    Statistic getStatistic();

    List<MeasurementAggregate> calculate(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Instant from,
            Instant to
    );
}