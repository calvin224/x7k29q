package com.calvinpower.weatherservice.repository;

import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    boolean existsBySensor_IdAndMetricAndRecordedAt(
            Long sensorId,
            Metric metric,
            Instant recordedAt
    );

    List<Measurement> findBySensor_IdInAndMetricInAndRecordedAtBetween(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Instant from,
            Instant to
    );

    @Query("""
            SELECT m
            FROM Measurement m
            WHERE m.metric IN :metrics
              AND m.recordedAt BETWEEN :from AND :to
            """)
    List<Measurement> findByMetricsAndRecordedAtBetween(
            @Param("metrics") Collection<Metric> metrics,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT m
            FROM Measurement m
            WHERE m.sensor.id IN :sensorIds
              AND m.metric IN :metrics
              AND m.recordedAt = (
                  SELECT MAX(m2.recordedAt)
                  FROM Measurement m2
                  WHERE m2.sensor.id = m.sensor.id
                    AND m2.metric = m.metric
              )
            """)
    List<Measurement> findLatestBySensorIdsAndMetrics(
            @Param("sensorIds") Collection<Long> sensorIds,
            @Param("metrics") Collection<Metric> metrics
    );

    @Query("""
            SELECT m
            FROM Measurement m
            WHERE m.metric IN :metrics
              AND m.recordedAt = (
                  SELECT MAX(m2.recordedAt)
                  FROM Measurement m2
                  WHERE m2.sensor.id = m.sensor.id
                    AND m2.metric = m.metric
              )
            """)
    List<Measurement> findLatestByMetrics(
            @Param("metrics") Collection<Metric> metrics
    );

    @Query("""
            SELECT MAX(m.recordedAt)
            FROM Measurement m
            WHERE m.sensor.id IN :sensorIds
              AND m.metric IN :metrics
            """)
    Optional<Instant> findLatestRecordedAtBySensorIdsAndMetrics(
            @Param("sensorIds") Collection<Long> sensorIds,
            @Param("metrics") Collection<Metric> metrics
    );

    @Query("""
            SELECT MAX(m.recordedAt)
            FROM Measurement m
            WHERE m.metric IN :metrics
            """)
    Optional<Instant> findLatestRecordedAtByMetrics(
            @Param("metrics") Collection<Metric> metrics
    );

    @Query("""
            SELECT new com.calvinpower.weatherservice.repository.MeasurementAggregate(
                m.sensor.id,
                m.metric,
                AVG(m.value)
            )
            FROM Measurement m
            WHERE m.sensor.id IN :sensorIds
              AND m.metric IN :metrics
              AND m.recordedAt BETWEEN :from AND :to
            GROUP BY m.sensor.id, m.metric
            """)
    List<MeasurementAggregate> findAverageBySensorsAndMetricsAndRecordedAtBetween(
            @Param("sensorIds") Collection<Long> sensorIds,
            @Param("metrics") Collection<Metric> metrics,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT new com.calvinpower.weatherservice.repository.MeasurementAggregate(
                m.sensor.id,
                m.metric,
                AVG(m.value)
            )
            FROM Measurement m
            WHERE m.metric IN :metrics
              AND m.recordedAt BETWEEN :from AND :to
            GROUP BY m.sensor.id, m.metric
            """)
    List<MeasurementAggregate> findAverageByMetricsAndRecordedAtBetween(
            @Param("metrics") Collection<Metric> metrics,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT new com.calvinpower.weatherservice.repository.MeasurementAggregate(
                m.sensor.id,
                m.metric,
                MIN(m.value)
            )
            FROM Measurement m
            WHERE m.sensor.id IN :sensorIds
              AND m.metric IN :metrics
              AND m.recordedAt BETWEEN :from AND :to
            GROUP BY m.sensor.id, m.metric
            """)
    List<MeasurementAggregate> findMinBySensorsAndMetricsAndRecordedAtBetween(
            @Param("sensorIds") Collection<Long> sensorIds,
            @Param("metrics") Collection<Metric> metrics,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT new com.calvinpower.weatherservice.repository.MeasurementAggregate(
                m.sensor.id,
                m.metric,
                MIN(m.value)
            )
            FROM Measurement m
            WHERE m.metric IN :metrics
              AND m.recordedAt BETWEEN :from AND :to
            GROUP BY m.sensor.id, m.metric
            """)
    List<MeasurementAggregate> findMinByMetricsAndRecordedAtBetween(
            @Param("metrics") Collection<Metric> metrics,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT new com.calvinpower.weatherservice.repository.MeasurementAggregate(
                m.sensor.id,
                m.metric,
                MAX(m.value)
            )
            FROM Measurement m
            WHERE m.sensor.id IN :sensorIds
              AND m.metric IN :metrics
              AND m.recordedAt BETWEEN :from AND :to
            GROUP BY m.sensor.id, m.metric
            """)
    List<MeasurementAggregate> findMaxBySensorsAndMetricsAndRecordedAtBetween(
            @Param("sensorIds") Collection<Long> sensorIds,
            @Param("metrics") Collection<Metric> metrics,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT new com.calvinpower.weatherservice.repository.MeasurementAggregate(
                m.sensor.id,
                m.metric,
                MAX(m.value)
            )
            FROM Measurement m
            WHERE m.metric IN :metrics
              AND m.recordedAt BETWEEN :from AND :to
            GROUP BY m.sensor.id, m.metric
            """)
    List<MeasurementAggregate> findMaxByMetricsAndRecordedAtBetween(
            @Param("metrics") Collection<Metric> metrics,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT new com.calvinpower.weatherservice.repository.MeasurementAggregate(
                m.sensor.id,
                m.metric,
                SUM(m.value)
            )
            FROM Measurement m
            WHERE m.sensor.id IN :sensorIds
              AND m.metric IN :metrics
              AND m.recordedAt BETWEEN :from AND :to
            GROUP BY m.sensor.id, m.metric
            """)
    List<MeasurementAggregate> findSumBySensorsAndMetricsAndRecordedAtBetween(
            @Param("sensorIds") Collection<Long> sensorIds,
            @Param("metrics") Collection<Metric> metrics,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT new com.calvinpower.weatherservice.repository.MeasurementAggregate(
                m.sensor.id,
                m.metric,
                SUM(m.value)
            )
            FROM Measurement m
            WHERE m.metric IN :metrics
              AND m.recordedAt BETWEEN :from AND :to
            GROUP BY m.sensor.id, m.metric
            """)
    List<MeasurementAggregate> findSumByMetricsAndRecordedAtBetween(
            @Param("metrics") Collection<Metric> metrics,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
