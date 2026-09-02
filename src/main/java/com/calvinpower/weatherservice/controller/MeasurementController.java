package com.calvinpower.weatherservice.controller;

import com.calvinpower.weatherservice.dto.*;
import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.services.MeasurementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @PostMapping("/sensors/{sensorId}/measurements")
    public ResponseEntity<MeasurementResponse> createMeasurement(
            @PathVariable Long sensorId,
            @Valid @RequestBody CreateMeasurementRequest request
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
    public ResponseEntity<List<MeasurementQueryResponse>> queryMeasurements(
            @Valid @RequestBody MeasurementQuery query
    ) {
        List<Measurement> measurements =
                measurementService.getMeasurements(
                        query.sensorIds(),
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

    @PostMapping("/statistics/query")
    public ResponseEntity<List<StatisticResponse>> queryStatistics(
            @Valid @RequestBody StatisticQuery query
    ) {
        List<MeasurementAggregate> aggregates =
                measurementService.getStatistics(
                        query.sensorIds(),
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
}