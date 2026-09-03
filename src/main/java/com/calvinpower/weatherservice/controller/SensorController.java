package com.calvinpower.weatherservice.controller;

import com.calvinpower.weatherservice.dto.CreateSensorRequest;
import com.calvinpower.weatherservice.dto.SensorResponse;
import com.calvinpower.weatherservice.model.Sensor;
import com.calvinpower.weatherservice.services.sensor.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sensors")
@Tag(
        name = "Sensors",
        description = "Register sensors that can submit weather measurements."
)
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping
    @Operation(
            summary = "List sensors",
            description = "Returns all registered sensors with their generated IDs and optional names."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registered sensors, or an empty array when none exist",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(implementation = SensorResponse.class)
                    )
            )
    )
    public ResponseEntity<List<SensorResponse>> getSensors() {
        List<SensorResponse> response = sensorService.getSensors().stream()
                .map(sensor -> new SensorResponse(
                        sensor.getId(),
                        sensor.getName()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(
            summary = "Create a sensor",
            description = """
                    Registers a sensor and returns its generated ID. The name is optional;
                    send an empty JSON object to create an unnamed sensor.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Sensor created",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SensorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid sensor name",
                    content = @Content
            )
    })
    public ResponseEntity<SensorResponse> createSensor(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Optional sensor name. Use `{}` for an unnamed sensor.",
                    required = true
            )
            @Valid @RequestBody CreateSensorRequest request
    ) {
        Sensor sensor =
                sensorService.createSensor(request.name());

        SensorResponse response =
                new SensorResponse(
                        sensor.getId(),
                        sensor.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
