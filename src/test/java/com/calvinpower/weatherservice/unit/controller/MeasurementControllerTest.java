package com.calvinpower.weatherservice.unit.controller;

import com.calvinpower.weatherservice.controller.MeasurementController;
import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.services.MeasurementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
    void given_valid_measurement_when_creating_then_returns_created() throws Exception {
        Measurement measurement = new Measurement(
                new com.calvinpower.weatherservice.model.Sensor(1L),
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-02T10:30:00Z")
        );

        when(measurementService.createMeasurement(
                1L,
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-02T10:30:00Z")
        )).thenReturn(measurement);

        mockMvc.perform(
                        post("/api/v1/sensors/1/measurements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "metric": "TEMPERATURE",
                                          "value": 21.5,
                                          "recordedAt": "2026-09-02T10:30:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.sensorId").value(1))
                .andExpect(jsonPath("$.metric").value("TEMPERATURE"))
                .andExpect(jsonPath("$.value").value(21.5))
                .andExpect(jsonPath("$.recordedAt")
                        .value("2026-09-02T10:30:00Z"));
    }

    @Test
    void given_missing_sensor_when_creating_measurement_then_returns_not_found()
            throws Exception {

        when(measurementService.createMeasurement(
                999L,
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-02T10:30:00Z")
        )).thenThrow(
                new com.calvinpower.weatherservice.exception.SensorNotFoundException(
                        999L
                )
        );

        mockMvc.perform(
                        post("/api/v1/sensors/999/measurements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "metric": "TEMPERATURE",
                                          "value": 21.5,
                                          "recordedAt": "2026-09-02T10:30:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void given_invalid_measurement_when_creating_then_returns_bad_request()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/sensors/1/measurements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "metric": null,
                                          "value": null,
                                          "recordedAt": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verify(measurementService, never())
                .createMeasurement(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void given_valid_measurement_query_when_querying_then_returns_ok()
            throws Exception {

        Measurement measurement = new Measurement(
                new com.calvinpower.weatherservice.model.Sensor(1L),
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-01T10:00:00Z")
        );

        when(measurementService.getMeasurements(
                List.of(1L),
                List.of(Metric.TEMPERATURE),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        )).thenReturn(List.of(measurement));

        mockMvc.perform(
                        post("/api/v1/measurements/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sensorIds": [1],
                                          "metrics": ["TEMPERATURE"],
                                          "from": "2026-09-01T00:00:00Z",
                                          "to": "2026-09-02T00:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensorId").value(1))
                .andExpect(jsonPath("$[0].metric")
                        .value("TEMPERATURE"))
                .andExpect(jsonPath("$[0].value").value(21.5))
                .andExpect(jsonPath("$[0].recordedAt")
                        .value("2026-09-01T10:00:00Z"));
    }

    @Test
    void given_empty_sensor_ids_when_querying_measurements_then_returns_measurements_from_all_sensors()
            throws Exception {

        Measurement firstMeasurement = new Measurement(
                new com.calvinpower.weatherservice.model.Sensor(1L),
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-01T10:00:00Z")
        );

        Measurement secondMeasurement = new Measurement(
                new com.calvinpower.weatherservice.model.Sensor(2L),
                Metric.TEMPERATURE,
                25.0,
                Instant.parse("2026-09-01T11:00:00Z")
        );

        when(measurementService.getMeasurements(
                List.of(),
                List.of(Metric.TEMPERATURE),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        )).thenReturn(List.of(
                firstMeasurement,
                secondMeasurement
        ));

        mockMvc.perform(
                        post("/api/v1/measurements/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sensorIds": [],
                                          "metrics": ["TEMPERATURE"],
                                          "from": "2026-09-01T00:00:00Z",
                                          "to": "2026-09-02T00:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sensorId").value(1))
                .andExpect(jsonPath("$[0].metric")
                        .value("TEMPERATURE"))
                .andExpect(jsonPath("$[0].value").value(21.5))
                .andExpect(jsonPath("$[1].sensorId").value(2))
                .andExpect(jsonPath("$[1].metric")
                        .value("TEMPERATURE"))
                .andExpect(jsonPath("$[1].value").value(25.0));

        verify(measurementService).getMeasurements(
                List.of(),
                List.of(Metric.TEMPERATURE),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        );
    }

    @Test
    void given_null_sensor_ids_when_querying_measurements_then_returns_bad_request()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/measurements/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sensorIds": null,
                                          "metrics": ["TEMPERATURE"],
                                          "from": "2026-09-01T00:00:00Z",
                                          "to": "2026-09-02T00:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verify(measurementService, never())
                .getMeasurements(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void given_valid_statistic_query_when_querying_then_returns_ok()
            throws Exception {

        when(measurementService.getStatistics(
                List.of(1L),
                List.of(Metric.TEMPERATURE),
                Statistic.AVERAGE,
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        )).thenReturn(List.of(
                new MeasurementAggregate(
                        1L,
                        Metric.TEMPERATURE,
                        21.5
                )
        ));

        mockMvc.perform(
                        post("/api/v1/statistics/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sensorIds": [1],
                                          "metrics": ["TEMPERATURE"],
                                          "statistic": "AVERAGE",
                                          "from": "2026-09-01T00:00:00Z",
                                          "to": "2026-09-02T00:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensorId").value(1))
                .andExpect(jsonPath("$[0].metric")
                        .value("TEMPERATURE"))
                .andExpect(jsonPath("$[0].statistic")
                        .value("AVERAGE"))
                .andExpect(jsonPath("$[0].value").value(21.5));
    }

    @Test
    void given_empty_metrics_when_querying_measurements_then_returns_bad_request()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/measurements/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sensorIds": [1],
                                          "metrics": [],
                                          "from": "2026-09-01T00:00:00Z",
                                          "to": "2026-09-02T00:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verify(measurementService, never())
                .getMeasurements(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void given_missing_statistic_when_querying_statistics_then_returns_bad_request()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/statistics/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sensorIds": [1],
                                          "metrics": ["TEMPERATURE"],
                                          "statistic": null,
                                          "from": "2026-09-01T00:00:00Z",
                                          "to": "2026-09-02T00:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verify(measurementService, never())
                .getStatistics(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }
}