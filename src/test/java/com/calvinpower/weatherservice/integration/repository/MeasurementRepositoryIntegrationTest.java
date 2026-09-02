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

    @Test
    void given_multiple_sensors_and_metrics_when_finding_measurements_then_returns_matching_measurements() {
        Sensor secondSensor = sensorRepository.save(new Sensor(null));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.HUMIDITY,
                60.0,
                Instant.parse("2026-09-01T10:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                sensor,
                Metric.PRESSURE,
                1012.0,
                Instant.parse("2026-09-01T10:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                sensor,
                Metric.TEMPERATURE,
                50.0,
                Instant.parse("2026-09-03T10:00:00Z")
        ));

        List<Measurement> results =
                measurementRepository
                        .findBySensor_IdInAndMetricInAndRecordedAtBetween(
                                List.of(sensor.getId(), secondSensor.getId()),
                                List.of(Metric.TEMPERATURE, Metric.HUMIDITY),
                                from,
                                to
                        );

        assertEquals(4, results.size());
    }

    @Test
    void given_multiple_measurements_when_finding_latest_then_returns_latest_per_sensor_and_metric() {
        Sensor secondSensor = sensorRepository.save(new Sensor(null));

        measurementRepository.save(new Measurement(
                sensor,
                Metric.TEMPERATURE,
                50.0,
                Instant.parse("2026-09-01T13:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                sensor,
                Metric.HUMIDITY,
                70.0,
                Instant.parse("2026-09-01T14:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.TEMPERATURE,
                25.0,
                Instant.parse("2026-09-01T15:00:00Z")
        ));

        List<Measurement> results =
                measurementRepository.findLatestBySensorIdsAndMetrics(
                        List.of(sensor.getId(), secondSensor.getId()),
                        List.of(Metric.TEMPERATURE, Metric.HUMIDITY)
                );

        assertEquals(3, results.size());

        assertEquals(50.0, results.stream()
                .filter(measurement ->
                        measurement.getSensor().getId().equals(sensor.getId())
                                && measurement.getMetric() == Metric.TEMPERATURE)
                .findFirst()
                .orElseThrow()
                .getValue());

        assertEquals(70.0, results.stream()
                .filter(measurement ->
                        measurement.getSensor().getId().equals(sensor.getId())
                                && measurement.getMetric() == Metric.HUMIDITY)
                .findFirst()
                .orElseThrow()
                .getValue());

        assertEquals(25.0, results.stream()
                .filter(measurement ->
                        measurement.getSensor().getId().equals(secondSensor.getId())
                                && measurement.getMetric() == Metric.TEMPERATURE)
                .findFirst()
                .orElseThrow()
                .getValue());
    }

    @Test
    void given_multiple_sensors_when_finding_all_measurements_then_returns_measurements_from_all_sensors() {
        Sensor secondSensor = sensorRepository.save(new Sensor(null));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.TEMPERATURE,
                25.0,
                Instant.parse("2026-09-01T10:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.HUMIDITY,
                65.0,
                Instant.parse("2026-09-01T11:00:00Z")
        ));

        List<Measurement> results =
                measurementRepository.findByMetricsAndRecordedAtBetween(
                        List.of(Metric.TEMPERATURE),
                        from,
                        to
                );

        assertEquals(4, results.size());

        assertEquals(3, results.stream()
                .filter(measurement ->
                        measurement.getSensor().getId().equals(sensor.getId()))
                .count());

        assertEquals(1, results.stream()
                .filter(measurement ->
                        measurement.getSensor().getId().equals(secondSensor.getId()))
                .count());
    }

    @Test
    void given_multiple_sensors_when_finding_latest_without_sensor_filter_then_returns_latest_per_sensor_and_metric() {
        Sensor secondSensor = sensorRepository.save(new Sensor(null));

        measurementRepository.save(new Measurement(
                sensor,
                Metric.TEMPERATURE,
                50.0,
                Instant.parse("2026-09-01T13:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                sensor,
                Metric.HUMIDITY,
                70.0,
                Instant.parse("2026-09-01T14:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.TEMPERATURE,
                25.0,
                Instant.parse("2026-09-01T15:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.TEMPERATURE,
                30.0,
                Instant.parse("2026-09-01T16:00:00Z")
        ));

        List<Measurement> results =
                measurementRepository.findLatestByMetrics(
                        List.of(Metric.TEMPERATURE, Metric.HUMIDITY)
                );

        assertEquals(3, results.size());

        assertEquals(50.0, results.stream()
                .filter(measurement ->
                        measurement.getSensor().getId().equals(sensor.getId())
                                && measurement.getMetric() == Metric.TEMPERATURE)
                .findFirst()
                .orElseThrow()
                .getValue());

        assertEquals(70.0, results.stream()
                .filter(measurement ->
                        measurement.getSensor().getId().equals(sensor.getId())
                                && measurement.getMetric() == Metric.HUMIDITY)
                .findFirst()
                .orElseThrow()
                .getValue());

        assertEquals(30.0, results.stream()
                .filter(measurement ->
                        measurement.getSensor().getId().equals(secondSensor.getId())
                                && measurement.getMetric() == Metric.TEMPERATURE)
                .findFirst()
                .orElseThrow()
                .getValue());
    }
}