ALTER TABLE sensor_measurements
    ADD CONSTRAINT uq_sensor_measurements_sensor_metric_recorded_at
        UNIQUE (sensor_id, metric, recorded_at);
