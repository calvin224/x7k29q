package com.calvinpower.weatherservice.unit.controller;

import com.calvinpower.weatherservice.controller.MeasurementController;
import com.calvinpower.weatherservice.exception.SensorNotFoundException;
import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Sensor;
import com.calvinpower.weatherservice.services.MeasurementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeasurementController.class)
class MeasurementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeasurementService measurementService;

    @Test
    void given_valid_measurement_when_creating_measurement_then_returns_created() throws Exception {
        Long sensorId = 1L;
        Sensor sensor = new Sensor(sensorId);

        Measurement measurement = new Measurement(
                sensor,
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-02T10:30:00Z")
        );

        when(measurementService.createMeasurement(
                eq(sensorId),
                eq(Metric.TEMPERATURE),
                eq(21.5),
                eq(Instant.parse("2026-09-02T10:30:00Z"))
        )).thenReturn(measurement);

        mockMvc.perform(
                        post("/api/v1/sensors/1/measurements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loadJson("requests/measurements/valid-temperature.json"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sensorId").value(1))
                .andExpect(jsonPath("$.metric").value("TEMPERATURE"))
                .andExpect(jsonPath("$.value").value(21.5))
                .andExpect(jsonPath("$.recordedAt")
                        .value("2026-09-02T10:30:00Z"));
    }

    @Test
    void given_sensor_does_not_exist_when_creating_measurement_then_returns_not_found() throws Exception {
        when(measurementService.createMeasurement(
                eq(1L),
                eq(Metric.TEMPERATURE),
                eq(21.5),
                eq(Instant.parse("2026-09-02T10:30:00Z"))
        )).thenThrow(new SensorNotFoundException(1L));

        mockMvc.perform(
                        post("/api/v1/sensors/1/measurements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loadJson(
                                        "requests/measurements/valid-temperature.json"
                                ))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void given_invalid_measurement_request_when_creating_measurement_then_returns_bad_request() throws Exception {
        mockMvc.perform(
                        post("/api/v1/sensors/1/measurements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loadJson("requests/measurements/invalid-measurement.json"))
                )
                .andExpect(status().isBadRequest());
    }

    private String loadJson(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);

        return Files.readString(resource.getFile().toPath());
    }
}