package com.calvinpower.weatherservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SensorResponse(
        @Schema(description = "Generated sensor ID.", example = "1")
        Long id,

        @Schema(
                description = "Sensor name, or null for an unnamed sensor.",
                example = "Dublin City Sensor",
                nullable = true
        )
        String name
) {
}
