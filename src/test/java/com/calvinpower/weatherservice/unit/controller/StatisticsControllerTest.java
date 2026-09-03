package com.calvinpower.weatherservice.unit.controller;

import com.calvinpower.weatherservice.controller.MeasurementController;
import com.calvinpower.weatherservice.controller.StatisticsController;
import com.calvinpower.weatherservice.exception.InvalidDateRangeException;
import com.calvinpower.weatherservice.exception.SensorNotFoundException;
import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Statistic;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.services.MeasurementService;
import com.calvinpower.weatherservice.services.sensor.SensorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.calvinpower.weatherservice.test_util.FixtureLoader.load;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    private static final String FIXTURE_ROOT =
            "fixtures/measurements/query_statistics/";

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
                                .content(fixture("statistic-query.json"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensorId").value(1))
                .andExpect(jsonPath("$[0].metric").value("TEMPERATURE"))
                .andExpect(jsonPath("$[0].statistic").value("AVERAGE"))
                .andExpect(jsonPath("$[0].value").value(21.5));
    }

    @Test
    void given_missing_sensor_when_querying_statistics_then_returns_structured_not_found()
            throws Exception {
        when(measurementService.getStatistics(
                List.of(999L),
                List.of(Metric.TEMPERATURE),
                Statistic.AVERAGE,
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        )).thenThrow(new SensorNotFoundException(999L));

        mockMvc.perform(
                        post("/api/v1/statistics/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture("missing-sensor.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Sensor not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Sensor 999 does not exist"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/v1/statistics/query"))
                .andExpect(jsonPath("$.code").value("SENSOR_NOT_FOUND"));
    }

    @Test
    void given_sensor_names_when_querying_statistics_then_returns_ok()
            throws Exception {
        when(sensorService.resolveSensorIds(
                List.of(),
                List.of("Dublin City Sensor")
        )).thenReturn(List.of(1L));
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
                                .content(fixture("by-sensor-names.json"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensorId").value(1))
                .andExpect(jsonPath("$[0].value").value(21.5));

        verify(sensorService).resolveSensorIds(
                List.of(),
                List.of("Dublin City Sensor")
        );
    }

    @Test
    void given_no_dates_when_querying_statistics_then_returns_latest_statistics()
            throws Exception {
        when(measurementService.getStatistics(
                List.of(1L),
                List.of(Metric.TEMPERATURE),
                Statistic.AVERAGE,
                null,
                null
        )).thenReturn(List.of(
                new MeasurementAggregate(
                        1L,
                        Metric.TEMPERATURE,
                        25.0
                )
        ));

        mockMvc.perform(
                        post("/api/v1/statistics/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture("latest-statistic-query.json"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensorId").value(1))
                .andExpect(jsonPath("$[0].metric").value("TEMPERATURE"))
                .andExpect(jsonPath("$[0].statistic").value("AVERAGE"))
                .andExpect(jsonPath("$[0].value").value(25.0));

        verify(measurementService).getStatistics(
                List.of(1L),
                List.of(Metric.TEMPERATURE),
                Statistic.AVERAGE,
                null,
                null
        );
    }

    @Test
    void given_partial_date_range_when_querying_statistics_then_returns_structured_bad_request()
            throws Exception {
        when(measurementService.getStatistics(
                List.of(1L),
                List.of(Metric.TEMPERATURE),
                Statistic.AVERAGE,
                Instant.parse("2026-09-01T00:00:00Z"),
                null
        )).thenThrow(new InvalidDateRangeException(
                "Both from and to must be provided together"
        ));

        mockMvc.perform(
                        post("/api/v1/statistics/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture("partial-date-range.json"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid date range"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("Both from and to must be provided together"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/v1/statistics/query"))
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void given_missing_statistic_when_querying_statistics_then_returns_validation_problem()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/statistics/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fixture("invalid-statistic.json"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("statistic must not be null"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(measurementService, never()).getStatistics(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void given_statistics_endpoint_when_inspecting_controller_organisation_then_only_statistics_controller_declares_it() {
        boolean measurementControllerDeclaresStatistics = Arrays.stream(
                        MeasurementController.class.getDeclaredMethods()
                )
                .anyMatch(method -> method.getName().equals("queryStatistics"));
        boolean statisticsControllerDeclaresStatistics = Arrays.stream(
                        StatisticsController.class.getDeclaredMethods()
                )
                .anyMatch(method -> method.getName().equals("queryStatistics"));

        assertFalse(measurementControllerDeclaresStatistics);
        assertTrue(statisticsControllerDeclaresStatistics);
        assertEquals(
                "Statistics",
                StatisticsController.class.getAnnotation(Tag.class).name()
        );
    }
}
