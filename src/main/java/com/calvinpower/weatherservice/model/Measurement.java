package com.calvinpower.weatherservice.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "sensor_measurements",
        indexes = {
                @Index(
                        name = "idx_sensor_measurements_sensor_metric_time",
                        columnList = "sensor_id, metric, recorded_at"
                )
        }
)
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Metric metric;

    @Column(nullable = false)
    private Double value;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected Measurement() {
    }

    public Measurement(
            Sensor sensor,
            Metric metric,
            Double value,
            Instant recordedAt
    ) {
        this.sensor = sensor;
        this.metric = metric;
        this.value = value;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public Metric getMetric() {
        return metric;
    }

    public Double getValue() {
        return value;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}