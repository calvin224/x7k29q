package com.calvinpower.weatherservice.repository;

import com.calvinpower.weatherservice.model.Measurement;
import com.calvinpower.weatherservice.model.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findBySensor_IdInAndMetricInAndRecordedAtBetween(
            Collection<Long> sensorIds,
            Collection<Metric> metrics,
            Instant from,
            Instant to
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
}