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
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MeasurementRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("weather")
                    .withUsername("weather")
                    .withPassword("weather");

    private final Instant from =
            Instant.parse("2026-09-01T00:00:00Z");

    private final Instant to =
            Instant.parse("2026-09-02T00:00:00Z");

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private SensorRepository sensorRepository;

    private Sensor sensor;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
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

    @Test
    void given_multiple_sensors_when_finding_average_without_sensor_filter_then_returns_average_per_sensor() {
        Sensor secondSensor = sensorRepository.save(new Sensor(null));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.TEMPERATURE,
                10.0,
                Instant.parse("2026-09-01T10:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.TEMPERATURE,
                50.0,
                Instant.parse("2026-09-01T11:00:00Z")
        ));

        List<MeasurementAggregate> results =
                measurementRepository.findAverageByMetricsAndRecordedAtBetween(
                        List.of(Metric.TEMPERATURE),
                        from,
                        to
                );

        assertEquals(2, results.size());

        assertEquals(30.0, results.stream()
                .filter(result ->
                        result.sensorId().equals(sensor.getId()))
                .findFirst()
                .orElseThrow()
                .value());

        assertEquals(30.0, results.stream()
                .filter(result ->
                        result.sensorId().equals(secondSensor.getId()))
                .findFirst()
                .orElseThrow()
                .value());
    }

    @Test
    void given_specific_sensors_and_metrics_when_finding_latest_timestamp_then_ignores_unrelated_data() {
        Sensor secondSensor = sensorRepository.save(new Sensor(null));
        Sensor unrelatedSensor = sensorRepository.save(new Sensor(null));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.TEMPERATURE,
                25.0,
                Instant.parse("2026-09-01T13:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.HUMIDITY,
                70.0,
                Instant.parse("2026-09-01T15:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                unrelatedSensor,
                Metric.TEMPERATURE,
                30.0,
                Instant.parse("2026-09-01T16:00:00Z")
        ));

        Optional<Instant> result = measurementRepository
                .findLatestRecordedAtBySensorIdsAndMetrics(
                        List.of(sensor.getId(), secondSensor.getId()),
                        List.of(Metric.TEMPERATURE)
                );

        assertEquals(
                Optional.of(Instant.parse("2026-09-01T13:00:00Z")),
                result
        );
    }

    @Test
    void given_all_sensors_and_requested_metrics_when_finding_latest_timestamp_then_ignores_unrelated_metrics() {
        Sensor secondSensor = sensorRepository.save(new Sensor(null));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.TEMPERATURE,
                25.0,
                Instant.parse("2026-09-01T14:00:00Z")
        ));

        measurementRepository.save(new Measurement(
                secondSensor,
                Metric.HUMIDITY,
                70.0,
                Instant.parse("2026-09-01T16:00:00Z")
        ));

        Optional<Instant> result = measurementRepository
                .findLatestRecordedAtByMetrics(
                        List.of(Metric.TEMPERATURE)
                );

        assertEquals(
                Optional.of(Instant.parse("2026-09-01T14:00:00Z")),
                result
        );
    }
}
