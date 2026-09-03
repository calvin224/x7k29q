package com.calvinpower.weatherservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;

@Schema(description = "Structured API error response")
public record ApiProblemResponse(
        @Schema(example = "Sensor not found")
        String title,

        @Schema(example = "404")
        int status,

        @Schema(example = "Sensor 999 does not exist")
        String detail,

        @Schema(example = "/api/v1/measurements/query")
        URI instance,

        @Schema(example = "SENSOR_NOT_FOUND")
        String code
) {
}
