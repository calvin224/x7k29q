package com.calvinpower.weatherservice.unit.controller;

import com.calvinpower.weatherservice.controller.MeasurementController;
import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.services.MeasurementService;
import com.calvinpower.weatherservice.services.sensor.SensorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static com.calvinpower.weatherservice.test_util.FixtureLoader.load;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeasurementController.class)
class MeasurementControllerTest {

    private static final String FIXTURE_ROOT = "fixtures/measurements/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeasurementService measurementService;

    @MockitoBean
    private SensorService sensorService;

    private static String fixture(String path) throws IOException {
        return load(FIXTURE_ROOT + path);
    }

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
                                .content(fixture("create_measurement/valid-temperature.json"))
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
    void given_sensor_name_when_creating_measurement_then_returns_created()
            throws Exception {
        com.calvinpower.weatherservice.model.Sensor sensor =
                new com.calvinpower.weatherservice.model.Sensor(
                        1L,
                        "Dublin City Sensor"
                );
        Measurement measurement = new Measurement(
                sensor,
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-02T10:30:00Z")
        );

        when(sensorService.getSensorByName("Dublin City Sensor"))
                .thenReturn(sensor);
        when(measurementService.createMeasurement(
                1L,
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-02T10:30:00Z")
        )).thenReturn(measurement);

        mockMvc.perform(
                        post(
                                "/api/v1/sensors/by-name/{sensorName}/measurements",
                                "Dublin City Sensor"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture("create_measurement/valid-temperature.json"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sensorId").value(1));

        verify(sensorService).getSensorByName("Dublin City Sensor");
    }

    @Test
    void given_unknown_sensor_name_when_creating_measurement_then_returns_not_found()
            throws Exception {
        when(sensorService.getSensorByName("Unknown Sensor"))
                .thenThrow(
                        new com.calvinpower.weatherservice.exception.SensorNotFoundException(
                                "Unknown Sensor"
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/sensors/by-name/{sensorName}/measurements",
                                "Unknown Sensor"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture("create_measurement/valid-temperature.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SENSOR_NOT_FOUND"));

        verify(measurementService, never()).createMeasurement(
                any(),
                any(),
                any(),
                any()
        );
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
                                .content(fixture("create_measurement/valid-temperature.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Sensor not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Sensor 999 does not exist"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/v1/sensors/999/measurements"))
                .andExpect(jsonPath("$.code").value("SENSOR_NOT_FOUND"));
    }

    @Test
    void given_invalid_measurement_when_creating_then_returns_bad_request()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/sensors/1/measurements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture("create_measurement/invalid-measurement.json"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

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
                                .content(fixture("query_measurements/query.json"))
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
    void given_existing_sensor_with_no_matching_data_when_querying_measurements_then_returns_empty_result()
            throws Exception {
        when(measurementService.getMeasurements(
                List.of(1L),
                List.of(Metric.TEMPERATURE),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        )).thenReturn(List.of());

        mockMvc.perform(
                        post("/api/v1/measurements/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture("query_measurements/query.json"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void given_missing_sensor_when_querying_measurements_then_returns_not_found()
            throws Exception {
        when(measurementService.getMeasurements(
                List.of(999L),
                List.of(Metric.TEMPERATURE),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        )).thenThrow(
                new com.calvinpower.weatherservice.exception.SensorNotFoundException(
                        999L
                )
        );

        mockMvc.perform(
                        post("/api/v1/measurements/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture(
                                        "query_measurements/missing-sensor.json"
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Sensor not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Sensor 999 does not exist"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/v1/measurements/query"))
                .andExpect(jsonPath("$.code").value("SENSOR_NOT_FOUND"));
    }

    @Test
    void given_sensor_names_when_querying_measurements_then_returns_ok()
            throws Exception {
        Measurement measurement = new Measurement(
                new com.calvinpower.weatherservice.model.Sensor(
                        1L,
                        "Dublin City Sensor"
                ),
                Metric.TEMPERATURE,
                21.5,
                Instant.parse("2026-09-01T10:00:00Z")
        );

        when(sensorService.resolveSensorIds(
                List.of(),
                List.of("Dublin City Sensor")
        )).thenReturn(List.of(1L));
        when(measurementService.getMeasurements(
                List.of(1L),
                List.of(Metric.TEMPERATURE),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        )).thenReturn(List.of(measurement));

        mockMvc.perform(
                        post("/api/v1/measurements/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture(
                                        "query_measurements/by-sensor-names.json"
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensorId").value(1));

        verify(sensorService).resolveSensorIds(
                List.of(),
                List.of("Dublin City Sensor")
        );
    }

    @Test
    void given_all_sensors_when_querying_measurements_then_returns_measurements_from_all_sensors()
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
                                .content(fixture("query_measurements/all-sensors.json"))
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
                                .content(fixture("query_measurements/invalid-query.json"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(measurementService, never())
                .getMeasurements(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void given_invalid_sensor_selection_when_querying_measurements_then_returns_specific_problem()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/measurements/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture(
                                        "query_measurements/invalid-sensor-selection.json"
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Invalid sensor selection"))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_SENSOR_SELECTION"));

        verify(measurementService, never()).getMeasurements(
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void given_empty_metrics_when_querying_measurements_then_returns_bad_request()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/measurements/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture("query_measurements/invalid-metrics.json"))
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

}
