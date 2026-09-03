package com.calvinpower.weatherservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSensorRequest(

        @Schema(
                description = "Optional human-readable sensor name. Omit the field for an unnamed sensor.",
                example = "Dublin City Sensor",
                maxLength = 100,
                nullable = true
        )
        @Size(max = 100)
        @Pattern(
                regexp = ".*\\S.*",
                message = "name must not be blank"
        )
        String name
) {
}
