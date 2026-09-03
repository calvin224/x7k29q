package com.calvinpower.weatherservice.controller;

import com.calvinpower.weatherservice.dto.ApiProblemResponse;
import com.calvinpower.weatherservice.dto.StatisticQuery;
import com.calvinpower.weatherservice.dto.StatisticResponse;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.services.MeasurementService;
import com.calvinpower.weatherservice.services.sensor.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@Tag(
        name = "Statistics",
        description = "Calculate aggregate values from recorded measurements."
)
public class StatisticsController {

    private final MeasurementService measurementService;
    private final SensorService sensorService;

    public StatisticsController(
            MeasurementService measurementService,
            SensorService sensorService
    ) {
        this.measurementService = measurementService;
        this.sensorService = sensorService;
    }

    @PostMapping("/query")
    @Operation(
            summary = "Calculate measurement statistics",
            description = """
                    Calculates MIN, MAX, SUM, or AVERAGE for each matching sensor and metric.
                    An explicit range must span at least one day and no more than one calendar month.
                    When both dates are omitted, the newest matching measurement becomes the end of
                    an implicit one-day range.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Calculated statistics grouped by sensor and metric",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(implementation = StatisticResponse.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filters, statistic, or date range",
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
    public ResponseEntity<List<StatisticResponse>> queryStatistics(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Sensor and metric filters, statistic, and optional date range.",
                    required = true
            )
            @Valid @RequestBody StatisticQuery query
    ) {
        List<Long> sensorIds = resolveSensorIds(
                query.sensorIds(),
                query.sensorNames()
        );

        List<MeasurementAggregate> aggregates =
                measurementService.getStatistics(
                        sensorIds,
                        query.metrics(),
                        query.statistic(),
                        query.from(),
                        query.to()
                );

        List<StatisticResponse> response = aggregates.stream()
                .map(aggregate -> new StatisticResponse(
                        aggregate.sensorId(),
                        aggregate.metric(),
                        query.statistic(),
                        aggregate.value()
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
