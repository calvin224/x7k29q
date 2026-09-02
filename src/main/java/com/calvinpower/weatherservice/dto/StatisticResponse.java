package com.calvinpower.weatherservice.dto;

import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;

public record StatisticResponse(
        Long sensorId,
        Metric metric,
        Statistic statistic,
        Double value
) {
}