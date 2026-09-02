package com.calvinpower.weatherservice.dto;

import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record StatisticQuery(
        @NotNull
        List<Long> sensorIds,

        @NotEmpty
        List<Metric> metrics,

        @NotNull
        Statistic statistic,

        Instant from,
        Instant to
) {
}