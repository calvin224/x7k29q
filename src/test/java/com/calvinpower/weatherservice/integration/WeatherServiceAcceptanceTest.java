package com.calvinpower.weatherservice.integration;

import com.calvinpower.weatherservice.model.Metric;
import com.calvinpower.weatherservice.repository.MeasurementRepository;
import com.calvinpower.weatherservice.repository.SensorRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WeatherServiceAcceptanceTest {

    private static final String MEASUREMENTS_QUERY_PATH =
            "/api/v1/measurements/query";
    private static final String STATISTICS_QUERY_PATH =
            "/api/v1/statistics/query";
    private static final Instant RANGE_FROM =
            Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant RANGE_TO =
            Instant.parse("2026-09-08T00:00:00Z");

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("weather")
                    .withUsername("weather")
                    .withPassword("weather");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        measurementRepository.deleteAllInBatch();
        sensorRepository.deleteAllInBatch();
    }

    @Test
    void createsSensorsAndRecordsChangingMeasurementsThroughTheHttpApi()
            throws Exception {
        long sensorId = createSensor("Challenge Sensor");

        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-09-01T12:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, 20,
                "2026-09-02T12:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, 30,
                "2026-09-03T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 40,
                "2026-09-01T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 60,
                "2026-09-02T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 80,
                "2026-09-03T12:00:00Z");

        HttpResponse<String> response = getJson("/api/v1/sensors");
        assertEquals(200, response.statusCode(), response.body());
        assertJsonContentType(response);

        List<Map<String, Object>> sensors = rows(response.body());
        assertTrue(sensors.stream().anyMatch(candidate ->
                ((Number) candidate.get("id")).longValue() == sensorId
                        && "Challenge Sensor".equals(candidate.get("name"))
        ));
    }

    @Test
    void oneSensorQueryExcludesOtherSensors() throws Exception {
        long selectedSensorId = createSensor("Selected Sensor");
        long otherSensorId = createSensor("Other Sensor");
        recordMeasurement(selectedSensorId, Metric.TEMPERATURE, 10,
                "2026-09-02T12:00:00Z");
        recordMeasurement(otherSensorId, Metric.TEMPERATURE, 99,
                "2026-09-02T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(selectedSensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(1, rows.size());
        assertMeasurement(rows, selectedSensorId, "TEMPERATURE", 10);
        assertFalse(hasRow(rows, otherSensorId, "TEMPERATURE"));
    }

    @Test
    void multipleSensorQueryReturnsOnlyTheSelectedSubset() throws Exception {
        long firstSensorId = createSensor("First Sensor");
        long secondSensorId = createSensor("Second Sensor");
        long unselectedSensorId = createSensor("Unselected Sensor");
        recordMeasurement(firstSensorId, Metric.TEMPERATURE, 10,
                "2026-09-02T12:00:00Z");
        recordMeasurement(secondSensorId, Metric.TEMPERATURE, 20,
                "2026-09-02T12:00:00Z");
        recordMeasurement(unselectedSensorId, Metric.TEMPERATURE, 30,
                "2026-09-02T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(firstSensorId, secondSensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(2, rows.size());
        assertMeasurement(rows, firstSensorId, "TEMPERATURE", 10);
        assertMeasurement(rows, secondSensorId, "TEMPERATURE", 20);
        assertFalse(hasRow(rows, unselectedSensorId, "TEMPERATURE"));
    }

    @Test
    void allSensorsQueryReturnsEverySensorWithMatchingData() throws Exception {
        long firstSensorId = createSensor("First Sensor");
        long secondSensorId = createSensor("Second Sensor");
        recordMeasurement(firstSensorId, Metric.TEMPERATURE, 10,
                "2026-09-02T12:00:00Z");
        recordMeasurement(secondSensorId, Metric.TEMPERATURE, 20,
                "2026-09-02T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        true,
                        List.of(),
                        List.of(),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(2, rows.size());
        assertMeasurement(rows, firstSensorId, "TEMPERATURE", 10);
        assertMeasurement(rows, secondSensorId, "TEMPERATURE", 20);
    }

    @Test
    void oneMetricQueryExcludesOtherMetrics() throws Exception {
        long sensorId = createSensor(null);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-09-02T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 40,
                "2026-09-02T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(1, rows.size());
        assertMeasurement(rows, sensorId, "TEMPERATURE", 10);
        assertFalse(hasRow(rows, sensorId, "HUMIDITY"));
    }

    @Test
    void multipleMetricQueryReturnsEachMetricSeparately() throws Exception {
        long sensorId = createSensor(null);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-09-02T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 40,
                "2026-09-02T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE", "HUMIDITY"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(2, rows.size());
        assertMeasurement(rows, sensorId, "TEMPERATURE", 10);
        assertMeasurement(rows, sensorId, "HUMIDITY", 40);
    }

    @ParameterizedTest(name = "{0} of 10, 20, 30 is {1}")
    @CsvSource({
            "AVERAGE, 20.0",
            "MIN, 10.0",
            "MAX, 30.0",
            "SUM, 60.0"
    })
    void calculatesEveryRequiredStatisticThroughTheFullStack(
            String statistic,
            double expectedValue
    ) throws Exception {
        long sensorId = createSensor(null);
        recordTemperatureSeries(sensorId, 10, 20, 30);

        List<Map<String, Object>> rows = queryStatistics(
                statisticQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        statistic,
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(1, rows.size());
        assertStatistic(rows, sensorId, "TEMPERATURE", expectedValue);
    }

    @Test
    void calculatesAverageTemperatureAndHumidityForOneSensorInTheLastWeek()
            throws Exception {
        long sensorId = createSensor("Specification Example Sensor");
        recordTemperatureSeries(sensorId, 10, 20, 30);
        recordMeasurement(sensorId, Metric.HUMIDITY, 40,
                "2026-09-01T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 60,
                "2026-09-02T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 80,
                "2026-09-03T12:00:00Z");

        List<Map<String, Object>> rows = queryStatistics(
                statisticQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE", "HUMIDITY"),
                        "AVERAGE",
                        Instant.parse("2026-08-28T00:00:00Z"),
                        Instant.parse("2026-09-04T00:00:00Z")
                )
        );

        assertEquals(2, rows.size());
        assertStatistic(rows, sensorId, "TEMPERATURE", 20);
        assertStatistic(rows, sensorId, "HUMIDITY", 60);
    }

    @Test
    void acceptsExactlyOneDay() throws Exception {
        long sensorId = createSensor(null);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-09-01T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        Instant.parse("2026-09-01T00:00:00Z"),
                        Instant.parse("2026-09-02T00:00:00Z")
                )
        );

        assertEquals(1, rows.size());
    }

    @Test
    void rejectsLessThanOneDay() throws Exception {
        long sensorId = createSensor(null);

        assertInvalidDateRange(measurementQuery(
                false,
                List.of(sensorId),
                List.of(),
                List.of("TEMPERATURE"),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-01T23:59:59Z")
        ));
    }

    @Test
    void acceptsExactlyOneCalendarMonth() throws Exception {
        long sensorId = createSensor(null);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-02-15T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        Instant.parse("2026-01-31T00:00:00Z"),
                        Instant.parse("2026-02-28T00:00:00Z")
                )
        );

        assertEquals(1, rows.size());
    }

    @Test
    void rejectsMoreThanOneCalendarMonth() throws Exception {
        long sensorId = createSensor(null);

        assertInvalidDateRange(measurementQuery(
                false,
                List.of(sensorId),
                List.of(),
                List.of("TEMPERATURE"),
                Instant.parse("2026-01-31T00:00:00Z"),
                Instant.parse("2026-02-28T00:00:01Z")
        ));
    }

    @Test
    void rejectsOnlyFromDate() throws Exception {
        long sensorId = createSensor(null);

        assertInvalidDateRange(measurementQuery(
                false,
                List.of(sensorId),
                List.of(),
                List.of("TEMPERATURE"),
                RANGE_FROM,
                null
        ));
    }

    @Test
    void rejectsOnlyToDate() throws Exception {
        long sensorId = createSensor(null);

        assertInvalidDateRange(measurementQuery(
                false,
                List.of(sensorId),
                List.of(),
                List.of("TEMPERATURE"),
                null,
                RANGE_TO
        ));
    }

    @Test
    void rawQueryWithoutDatesReturnsOnlyTheLatestReading() throws Exception {
        long sensorId = createSensor(null);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-09-01T10:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, 20,
                "2026-09-01T11:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, 30,
                "2026-09-01T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        null,
                        null
                )
        );

        assertEquals(1, rows.size());
        assertMeasurement(rows, sensorId, "TEMPERATURE", 30);
    }

    @Test
    void latestReadingIsResolvedIndependentlyPerMetric() throws Exception {
        long sensorId = createSensor(null);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-09-01T10:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, 30,
                "2026-09-01T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 40,
                "2026-09-01T09:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 60,
                "2026-09-01T11:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE", "HUMIDITY"),
                        null,
                        null
                )
        );

        assertEquals(2, rows.size());
        assertMeasurement(rows, sensorId, "TEMPERATURE", 30);
        assertMeasurement(rows, sensorId, "HUMIDITY", 60);
    }

    @Test
    void latestReadingIsResolvedIndependentlyPerSensor() throws Exception {
        long firstSensorId = createSensor(null);
        long secondSensorId = createSensor(null);
        recordMeasurement(firstSensorId, Metric.TEMPERATURE, 10,
                "2026-09-01T10:00:00Z");
        recordMeasurement(firstSensorId, Metric.TEMPERATURE, 20,
                "2026-09-01T11:00:00Z");
        recordMeasurement(secondSensorId, Metric.TEMPERATURE, 100,
                "2026-09-01T09:00:00Z");
        recordMeasurement(secondSensorId, Metric.TEMPERATURE, 200,
                "2026-09-01T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(firstSensorId, secondSensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        null,
                        null
                )
        );

        assertEquals(2, rows.size());
        assertMeasurement(rows, firstSensorId, "TEMPERATURE", 20);
        assertMeasurement(rows, secondSensorId, "TEMPERATURE", 200);
    }

    @Test
    void statisticsWithoutDatesUseTheLatestMatchingOneDayWindow()
            throws Exception {
        long sensorId = createSensor(null);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 100,
                "2026-09-01T12:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, 20,
                "2026-09-02T12:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, 30,
                "2026-09-03T12:00:00Z");

        List<Map<String, Object>> rows = queryStatistics(
                statisticQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        "AVERAGE",
                        null,
                        null
                )
        );

        assertEquals(1, rows.size());
        assertStatistic(rows, sensorId, "TEMPERATURE", 25);
    }

    @Test
    void statisticsRemainGroupedBySensor() throws Exception {
        long firstSensorId = createSensor(null);
        long secondSensorId = createSensor(null);
        recordMeasurement(firstSensorId, Metric.TEMPERATURE, 10,
                "2026-09-01T12:00:00Z");
        recordMeasurement(firstSensorId, Metric.TEMPERATURE, 20,
                "2026-09-02T12:00:00Z");
        recordMeasurement(secondSensorId, Metric.TEMPERATURE, 100,
                "2026-09-01T12:00:00Z");
        recordMeasurement(secondSensorId, Metric.TEMPERATURE, 200,
                "2026-09-02T12:00:00Z");

        List<Map<String, Object>> rows = queryStatistics(
                statisticQuery(
                        false,
                        List.of(firstSensorId, secondSensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        "AVERAGE",
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(2, rows.size());
        assertStatistic(rows, firstSensorId, "TEMPERATURE", 15);
        assertStatistic(rows, secondSensorId, "TEMPERATURE", 150);
    }

    @Test
    void statisticsRemainGroupedByMetric() throws Exception {
        long sensorId = createSensor(null);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-09-01T12:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, 20,
                "2026-09-02T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 40,
                "2026-09-01T12:00:00Z");
        recordMeasurement(sensorId, Metric.HUMIDITY, 60,
                "2026-09-02T12:00:00Z");

        List<Map<String, Object>> rows = queryStatistics(
                statisticQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE", "HUMIDITY"),
                        "AVERAGE",
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(2, rows.size());
        assertStatistic(rows, sensorId, "TEMPERATURE", 15);
        assertStatistic(rows, sensorId, "HUMIDITY", 50);
    }

    @Test
    void existingSensorWithNoMatchingDataReturnsAnEmptyList() throws Exception {
        long sensorId = createSensor(null);

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertTrue(rows.isEmpty());
    }

    @Test
    void missingSensorIdReturnsNotFound() throws Exception {
        long existingSensorId = createSensor(null);
        long missingSensorId = existingSensorId + 1_000_000;

        HttpResponse<String> response = postJson(
                MEASUREMENTS_QUERY_PATH,
                measurementQuery(
                        false,
                        List.of(missingSensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertProblem(response, 404, "SENSOR_NOT_FOUND");
    }

    @Test
    void mixedExistingAndMissingSensorIdsReturnNoPartialResults()
            throws Exception {
        long existingSensorId = createSensor(null);
        long missingSensorId = existingSensorId + 1_000_000;
        recordMeasurement(existingSensorId, Metric.TEMPERATURE, 10,
                "2026-09-02T12:00:00Z");

        HttpResponse<String> response = postJson(
                MEASUREMENTS_QUERY_PATH,
                measurementQuery(
                        false,
                        List.of(existingSensorId, missingSensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertProblem(response, 404, "SENSOR_NOT_FOUND");
    }

    @Test
    void duplicateSensorIdsDoNotDuplicateResults() throws Exception {
        long sensorId = createSensor(null);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-09-02T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(sensorId, sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(1, rows.size());
        assertMeasurement(rows, sensorId, "TEMPERATURE", 10);
    }

    @Test
    void sensorCanBeSelectedByExactName() throws Exception {
        String sensorName = "Dublin City Sensor";
        long sensorId = createSensor(sensorName);
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10,
                "2026-09-02T12:00:00Z");

        List<Map<String, Object>> rows = queryMeasurements(
                measurementQuery(
                        false,
                        List.of(),
                        List.of(sensorName),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertEquals(1, rows.size());
        assertMeasurement(rows, sensorId, "TEMPERATURE", 10);
    }

    @Test
    void unknownSensorNameReturnsNotFound() throws Exception {
        HttpResponse<String> response = postJson(
                MEASUREMENTS_QUERY_PATH,
                measurementQuery(
                        false,
                        List.of(),
                        List.of("Unknown Sensor"),
                        List.of("TEMPERATURE"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );

        assertProblem(response, 404, "SENSOR_NOT_FOUND");
    }

    @Test
    void allSensorsWithSpecificIdsReturnsClearValidationError()
            throws Exception {
        long sensorId = createSensor(null);

        HttpResponse<String> response = postJson(
                MEASUREMENTS_QUERY_PATH,
                measurementQuery(
                        true,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        null,
                        null
                )
        );

        assertProblem(response, 400, "INVALID_SENSOR_SELECTION");
        assertEquals(
                "Invalid sensor selection",
                JsonPath.read(response.body(), "$.title")
        );
        assertEquals(
                "When allSensors is true, sensorIds and sensorNames must be empty",
                JsonPath.read(response.body(), "$.detail")
        );
        assertNoInternalDetails(response.body());
    }

    @Test
    void noSpecificSensorsReturnsClearValidationError() throws Exception {
        HttpResponse<String> response = postJson(
                STATISTICS_QUERY_PATH,
                statisticQuery(
                        false,
                        List.of(),
                        List.of(),
                        List.of("TEMPERATURE"),
                        "AVERAGE",
                        null,
                        null
                )
        );

        assertProblem(response, 400, "INVALID_SENSOR_SELECTION");
        assertEquals(
                "Invalid sensor selection",
                JsonPath.read(response.body(), "$.title")
        );
        assertEquals(
                "When allSensors is false, at least one sensor ID or sensor name must be provided",
                JsonPath.read(response.body(), "$.detail")
        );
        assertNoInternalDetails(response.body());
    }

    @Test
    void missingMetricsReturnsValidationFailure() throws Exception {
        long sensorId = createSensor(null);

        assertValidationFailure(
                MEASUREMENTS_QUERY_PATH,
                measurementQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        null,
                        RANGE_FROM,
                        RANGE_TO
                )
        );
    }

    @Test
    void invalidMetricEnumReturnsValidationFailure() throws Exception {
        long sensorId = createSensor(null);

        assertValidationFailure(
                MEASUREMENTS_QUERY_PATH,
                measurementQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("NOT_A_METRIC"),
                        RANGE_FROM,
                        RANGE_TO
                )
        );
    }

    @Test
    void invalidStatisticEnumReturnsValidationFailure() throws Exception {
        long sensorId = createSensor(null);

        assertValidationFailure(
                STATISTICS_QUERY_PATH,
                statisticQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        "MEDIAN",
                        RANGE_FROM,
                        RANGE_TO
                )
        );
    }

    @Test
    void missingStatisticReturnsValidationFailure() throws Exception {
        long sensorId = createSensor(null);

        assertValidationFailure(
                STATISTICS_QUERY_PATH,
                statisticQuery(
                        false,
                        List.of(sensorId),
                        List.of(),
                        List.of("TEMPERATURE"),
                        null,
                        RANGE_FROM,
                        RANGE_TO
                )
        );
    }

    @Test
    void malformedJsonReturnsValidationFailureWithoutInternalDetails()
            throws Exception {
        assertValidationFailure(
                MEASUREMENTS_QUERY_PATH,
                "{\"allSensors\": false"
        );
    }

    @Test
    void exactDuplicateMeasurementReturnsConflict() throws Exception {
        long sensorId = createSensor(null);
        String recordedAt = "2026-09-04T12:00:00Z";
        recordMeasurement(sensorId, Metric.TEMPERATURE, 10, recordedAt);

        HttpResponse<String> response = postMeasurement(
                sensorId,
                Metric.TEMPERATURE,
                20,
                recordedAt
        );

        assertProblem(response, 409, "DUPLICATE_MEASUREMENT");
        assertEquals(
                "Measurement already exists",
                JsonPath.read(response.body(), "$.title")
        );
        assertEquals(
                "A TEMPERATURE measurement already exists for this sensor at the supplied timestamp",
                JsonPath.read(response.body(), "$.detail")
        );
        assertNoInternalDetails(response.body());
    }

    @Test
    void sameTimestampWithDifferentMetricSucceeds() throws Exception {
        long sensorId = createSensor(null);
        String recordedAt = "2026-09-04T12:00:00Z";

        recordMeasurement(sensorId, Metric.TEMPERATURE, 10, recordedAt);
        recordMeasurement(sensorId, Metric.HUMIDITY, 40, recordedAt);
    }

    @Test
    void sameMetricAndTimestampForDifferentSensorsSucceeds() throws Exception {
        long firstSensorId = createSensor(null);
        long secondSensorId = createSensor(null);
        String recordedAt = "2026-09-04T12:00:00Z";

        recordMeasurement(firstSensorId, Metric.TEMPERATURE, 10, recordedAt);
        recordMeasurement(secondSensorId, Metric.TEMPERATURE, 20, recordedAt);
    }

    private long createSensor(String name) throws Exception {
        String request = name == null
                ? "{}"
                : "{\"name\":\"" + name + "\"}";

        HttpResponse<String> response = postJson("/api/v1/sensors", request);

        assertEquals(201, response.statusCode(), response.body());
        assertJsonContentType(response);

        Number sensorId = JsonPath.read(
                response.body(),
                "$.id"
        );
        assertNotNull(sensorId);
        assertEquals(name, JsonPath.read(response.body(), "$.name"));
        return sensorId.longValue();
    }

    private void recordTemperatureSeries(
            long sensorId,
            double first,
            double second,
            double third
    ) throws Exception {
        recordMeasurement(sensorId, Metric.TEMPERATURE, first,
                "2026-09-01T12:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, second,
                "2026-09-02T12:00:00Z");
        recordMeasurement(sensorId, Metric.TEMPERATURE, third,
                "2026-09-03T12:00:00Z");
    }

    private void recordMeasurement(
            long sensorId,
            Metric metric,
            double value,
            String recordedAt
    ) throws Exception {
        HttpResponse<String> response = postMeasurement(
                sensorId,
                metric,
                value,
                recordedAt
        );

        assertEquals(201, response.statusCode(), response.body());
        assertJsonContentType(response);
        assertNotNull(JsonPath.read(response.body(), "$.id"));
        assertEquals(
                sensorId,
                ((Number) JsonPath.read(
                        response.body(),
                        "$.sensorId"
                )).longValue()
        );
        assertEquals(metric.name(), JsonPath.read(
                response.body(),
                "$.metric"
        ));
        assertEquals(
                value,
                ((Number) JsonPath.read(
                        response.body(),
                        "$.value"
                )).doubleValue(),
                0.0001
        );
        assertEquals(recordedAt, JsonPath.read(
                response.body(),
                "$.recordedAt"
        ));
    }

    private HttpResponse<String> postMeasurement(
            long sensorId,
            Metric metric,
            double value,
            String recordedAt
    ) throws Exception {
        return postJson(
                "/api/v1/sensors/" + sensorId + "/measurements",
                """
                {
                  "metric": "%s",
                  "value": %s,
                  "recordedAt": "%s"
                }
                """.formatted(metric, value, recordedAt)
        );
    }

    private List<Map<String, Object>> queryMeasurements(String request)
            throws Exception {
        HttpResponse<String> response = postJson(
                MEASUREMENTS_QUERY_PATH,
                request
        );

        assertEquals(200, response.statusCode(), response.body());
        assertJsonContentType(response);
        return rows(response.body());
    }

    private List<Map<String, Object>> queryStatistics(String request)
            throws Exception {
        HttpResponse<String> response = postJson(
                STATISTICS_QUERY_PATH,
                request
        );

        assertEquals(200, response.statusCode(), response.body());
        assertJsonContentType(response);
        return rows(response.body());
    }

    private void assertInvalidDateRange(String request) throws Exception {
        HttpResponse<String> response = postJson(
                MEASUREMENTS_QUERY_PATH,
                request
        );

        assertProblem(response, 400, "INVALID_DATE_RANGE");
        assertNoInternalDetails(response.body());
    }

    private void assertValidationFailure(String path, String request)
            throws Exception {
        HttpResponse<String> response = postJson(path, request);

        assertProblem(response, 400, "VALIDATION_FAILED");
        assertNoInternalDetails(response.body());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(String response) {
        return JsonPath.read(response, "$");
    }

    private void assertMeasurement(
            List<Map<String, Object>> rows,
            long sensorId,
            String metric,
            double expectedValue
    ) {
        Map<String, Object> row = findRow(rows, sensorId, metric);
        assertEquals(
                expectedValue,
                ((Number) row.get("value")).doubleValue(),
                0.0001
        );
        assertNotNull(row.get("recordedAt"));
    }

    private void assertStatistic(
            List<Map<String, Object>> rows,
            long sensorId,
            String metric,
            double expectedValue
    ) {
        Map<String, Object> row = findRow(rows, sensorId, metric);
        assertEquals(
                expectedValue,
                ((Number) row.get("value")).doubleValue(),
                0.0001
        );
    }

    private Map<String, Object> findRow(
            List<Map<String, Object>> rows,
            long sensorId,
            String metric
    ) {
        return rows.stream()
                .filter(row -> ((Number) row.get("sensorId")).longValue()
                        == sensorId)
                .filter(row -> metric.equals(row.get("metric")))
                .findFirst()
                .orElseThrow();
    }

    private boolean hasRow(
            List<Map<String, Object>> rows,
            long sensorId,
            String metric
    ) {
        return rows.stream()
                .anyMatch(row -> ((Number) row.get("sensorId")).longValue()
                        == sensorId
                        && metric.equals(row.get("metric")));
    }

    private void assertNoInternalDetails(String responseBody) {
        String response = responseBody.toLowerCase();

        Set<String> forbiddenDetails = Set.of(
                "sensorselectionvalid",
                "jackson",
                "hibernate",
                "postgresql",
                "stacktrace",
                "constraint",
                "org.springframework",
                "com.calvinpower"
        );

        forbiddenDetails.forEach(detail ->
                assertFalse(response.contains(detail), response)
        );
    }

    private HttpResponse<String> postJson(String path, String request)
            throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(request))
                .build();

        return httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> getJson(String path) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder(uri(path))
                .GET()
                .build();

        return httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private void assertProblem(
            HttpResponse<String> response,
            int expectedStatus,
            String expectedCode
    ) {
        assertEquals(expectedStatus, response.statusCode(), response.body());
        assertContentType(response, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertEquals(expectedCode, JsonPath.read(response.body(), "$.code"));
    }

    private void assertJsonContentType(HttpResponse<String> response) {
        assertContentType(response, MediaType.APPLICATION_JSON_VALUE);
    }

    private void assertContentType(
            HttpResponse<String> response,
            String expectedContentType
    ) {
        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertTrue(
                contentType.startsWith(expectedContentType),
                contentType
        );
    }

    private String measurementQuery(
            boolean allSensors,
            List<Long> sensorIds,
            List<String> sensorNames,
            List<String> metrics,
            Instant from,
            Instant to
    ) {
        return queryJson(
                allSensors,
                sensorIds,
                sensorNames,
                metrics,
                null,
                from,
                to
        );
    }

    private String statisticQuery(
            boolean allSensors,
            List<Long> sensorIds,
            List<String> sensorNames,
            List<String> metrics,
            String statistic,
            Instant from,
            Instant to
    ) {
        return queryJson(
                allSensors,
                sensorIds,
                sensorNames,
                metrics,
                statistic,
                from,
                to
        );
    }

    private String queryJson(
            boolean allSensors,
            List<Long> sensorIds,
            List<String> sensorNames,
            List<String> metrics,
            String statistic,
            Instant from,
            Instant to
    ) {
        StringBuilder request = new StringBuilder("{")
                .append("\"allSensors\":").append(allSensors)
                .append(",\"sensorIds\":").append(sensorIds)
                .append(",\"sensorNames\":")
                .append(stringArray(sensorNames));

        if (metrics != null) {
            request.append(",\"metrics\":")
                    .append(stringArray(metrics));
        }

        if (statistic != null) {
            request.append(",\"statistic\":\"")
                    .append(statistic)
                    .append('"');
        }

        if (from != null) {
            request.append(",\"from\":\"")
                    .append(from)
                    .append('"');
        }

        if (to != null) {
            request.append(",\"to\":\"")
                    .append(to)
                    .append('"');
        }

        return request.append('}').toString();
    }

    private String stringArray(Collection<String> values) {
        return values.stream()
                .map(value -> "\"" + value + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
