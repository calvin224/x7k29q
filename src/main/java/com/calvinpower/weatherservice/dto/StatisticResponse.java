package com.calvinpower.weatherservice.dto;

import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import io.swagger.v3.oas.annotations.media.Schema;

public record StatisticResponse(
        @Schema(description = "Sensor whose readings were aggregated.", example = "1")
        Long sensorId,

        @Schema(description = "Metric that was aggregated.", example = "TEMPERATURE")
        Metric metric,

        @Schema(description = "Aggregation applied to the readings.", example = "AVERAGE")
        Statistic statistic,

        @Schema(description = "Calculated aggregate value.", example = "21.5")
        Double value
) {
}
