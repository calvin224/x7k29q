package com.calvinpower.weatherservice.controller;

import com.calvinpower.weatherservice.dto.*;
import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Sensor;
import com.calvinpower.weatherservice.services.MeasurementService;
import com.calvinpower.weatherservice.services.sensor.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(
        name = "Measurements",
        description = "Record and retrieve weather measurements."
)
public class MeasurementController {

    private final MeasurementService measurementService;
    private final SensorService sensorService;

    public MeasurementController(
            MeasurementService measurementService,
            SensorService sensorService
    ) {
        this.measurementService = measurementService;
        this.sensorService = sensorService;
    }

    @PostMapping("/sensors/{sensorId}/measurements")
    @Operation(
            summary = "Record a measurement",
            description = "Adds a timestamped weather reading to an existing sensor."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Measurement recorded",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeasurementResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid measurement payload",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Sensor not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponse.class)
                    )
            )
    })
    public ResponseEntity<MeasurementResponse> createMeasurement(
            @Parameter(
                    description = "ID of the sensor that produced the reading",
                    example = "1"
            )
            @PathVariable Long sensorId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Metric, numeric reading, and ISO-8601 timestamp.",
                    required = true
            )
            @Valid @RequestBody CreateMeasurementRequest request
    ) {
        return recordMeasurement(sensorId, request);
    }

    @PostMapping("/sensors/by-name/{sensorName}/measurements")
    @Operation(
            summary = "Record a measurement by sensor name",
            description = "Adds a timestamped weather reading using the sensor's exact name."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Measurement recorded",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeasurementResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid measurement payload",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Sensor name not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponse.class)
                    )
            )
    })
    public ResponseEntity<MeasurementResponse> createMeasurementBySensorName(
            @Parameter(
                    description = "Exact name of the sensor that produced the reading",
                    example = "Dublin City Sensor"
            )
            @PathVariable String sensorName,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Metric, numeric reading, and ISO-8601 timestamp.",
                    required = true
            )
            @Valid @RequestBody CreateMeasurementRequest request
    ) {
        Sensor sensor = sensorService.getSensorByName(sensorName);
        return recordMeasurement(sensor.getId(), request);
    }

    private ResponseEntity<MeasurementResponse> recordMeasurement(
            Long sensorId,
            CreateMeasurementRequest request
    ) {
        Measurement measurement = measurementService.createMeasurement(
                sensorId,
                request.metric(),
                request.value(),
                request.recordedAt()
        );

        MeasurementResponse response = new MeasurementResponse(
                measurement.getId(),
                measurement.getSensor().getId(),
                measurement.getMetric(),
                measurement.getValue(),
                measurement.getRecordedAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/measurements/query")
    @Operation(
            summary = "Query measurements",
            description = """
                    Returns readings matching the selected sensors and metrics. Provide both `from`
                    and `to` for a bounded range of one day to one calendar month. Omit both dates
                    to return only the latest reading for each selected sensor and metric.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Matching measurements",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(implementation = MeasurementQueryResponse.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filters or date range",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "A requested sensor ID or name was not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponse.class)
                    )
            )
    })
    public ResponseEntity<List<MeasurementQueryResponse>> queryMeasurements(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Sensor, metric, and optional date-range filters.",
                    required = true
            )
            @Valid @RequestBody MeasurementQuery query
    ) {
        List<Long> sensorIds = resolveSensorIds(
                query.sensorIds(),
                query.sensorNames()
        );

        List<Measurement> measurements =
                measurementService.getMeasurements(
                        sensorIds,
                        query.metrics(),
                        query.from(),
                        query.to()
                );

        List<MeasurementQueryResponse> response = measurements.stream()
                .map(measurement -> new MeasurementQueryResponse(
                        measurement.getSensor().getId(),
                        measurement.getMetric(),
                        measurement.getValue(),
                        measurement.getRecordedAt()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    private List<Long> resolveSensorIds(
            List<Long> sensorIds,
            List<String> sensorNames
    ) {
        if (sensorNames == null || sensorNames.isEmpty()) {
            return sensorIds;
        }

        return sensorService.resolveSensorIds(sensorIds, sensorNames);
    }
}
