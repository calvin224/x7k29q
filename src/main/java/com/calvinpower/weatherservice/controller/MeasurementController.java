package com.calvinpower.weatherservice.controller;

import com.calvinpower.weatherservice.dto.CreateMeasurementRequest;
import com.calvinpower.weatherservice.dto.MeasurementResponse;
import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.services.MeasurementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensors")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @PostMapping("/{sensorId}/measurements")
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
}