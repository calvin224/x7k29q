package com.calvinpower.weatherservice.integration.repository;

import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.model.Sensor;
import com.calvinpower.weatherservice.repository.MeasurementAggregate;
import com.calvinpower.weatherservice.repository.MeasurementRepository;
import com.calvinpower.weatherservice.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class MeasurementRepositoryIntegrationTest {

    private final Instant from =
            Instant.parse("2026-09-01T00:00:00Z");

    private final Instant to =
            Instant.parse("2026-09-02T00:00:00Z");

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private SensorRepository sensorRepository;

    private Sensor sensor;

    @BeforeEach
    void setUp() {
        measurementRepository.deleteAll();
        sensorRepository.deleteAll();

        sensor = sensorRepository.save(new Sensor(null));

        measurementRepository.save(new Measurement(
                sensor,
                Metric.TEMPERATURE,
                20.0,
                Instant.parse("2026-09-01T10:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                sensor,
                Metric.TEMPERATURE,
                30.0,
                Instant.parse("2026-09-01T11:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                sensor,
                Metric.TEMPERATURE,
                40.0,
                Instant.parse("2026-09-01T12:00:00Z")
        ));
    }

    @Test
    void given_temperature_measurements_when_finding_average_then_returns_average() {
        List<MeasurementAggregate> results =
                measurementRepository
                        .findAverageBySensorsAndMetricsAndRecordedAtBetween(
                                List.of(sensor.getId()),
                                List.of(Metric.TEMPERATURE),
                                from,
                                to
                        );

        assertEquals(1, results.size());
        assertEquals(sensor.getId(), results.getFirst().sensorId());
        assertEquals(Metric.TEMPERATURE, results.getFirst().metric());
        assertEquals(30.0, results.getFirst().value());
    }

    @Test
    void given_temperature_measurements_when_finding_min_then_returns_minimum() {
        List<MeasurementAggregate> results =
                measurementRepository
                        .findMinBySensorsAndMetricsAndRecordedAtBetween(
                                List.of(sensor.getId()),
                                List.of(Metric.TEMPERATURE),
                                from,
                                to
                        );

        assertEquals(1, results.size());
        assertEquals(20.0, results.getFirst().value());
    }

    @Test
    void given_temperature_measurements_when_finding_max_then_returns_maximum() {
        List<MeasurementAggregate> results =
                measurementRepository
                        .findMaxBySensorsAndMetricsAndRecordedAtBetween(
                                List.of(sensor.getId()),
                                List.of(Metric.TEMPERATURE),
                                from,
                                to
                        );

        assertEquals(1, results.size());
        assertEquals(40.0, results.getFirst().value());
    }

    @Test
    void given_temperature_measurements_when_finding_sum_then_returns_sum() {
        List<MeasurementAggregate> results =
                measurementRepository
                        .findSumBySensorsAndMetricsAndRecordedAtBetween(
                                List.of(sensor.getId()),
                                List.of(Metric.TEMPERATURE),
                                from,
                                to
                        );

        assertEquals(1, results.size());
        assertEquals(90.0, results.getFirst().value());
    }
}